package com.guoyu.platform.trainingdepartment.service.shiftassign.engine;

import com.guoyu.platform.trainingdepartment.service.shiftassign.ShiftAssignDefaults;
import com.guoyu.platform.trainingdepartment.service.shiftassign.config.EffectiveSchedulingConfig;
import com.guoyu.platform.trainingdepartment.service.shiftassign.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 分班排课核心算法：可排课 → 集中排课 → 理论/实操日 → 拆班 → 老师排课
 */
@Service
public class ScheduleAlgorithmService {

    public ClassAssignResult runClassAssign(ScheduleParseResult parse) {
        return runClassAssign(parse, ShiftAssignDefaults.config());
    }

    public ClassAssignResult runClassAssign(ScheduleParseResult parse, EffectiveSchedulingConfig cfg) {
        if (cfg == null) cfg = ShiftAssignDefaults.config();
        int year = parse.getYear();
        int month = parse.getMonth();
        String factoryId = parse.getFactoryId();
        Map<String, ShiftTimeDef> shiftMap = parse.getShiftTimeMap();
        List<PersonDaySlot> slots = parse.getPersonDaySlots();
        int daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth();
        // 理论日、实操日仅在本月内；跨月场景不支持，需分月排课

        // 理论日：优先周末，取前12个
        List<LocalDate> theoryDays = theoryDays(year, month);
        List<LocalDate> practiceDays = practiceDays(year, month, theoryDays);

        // 每人每日上午/下午是否可排课（R1 R2 R3）；人员唯一键：有工号用工号，无工号用姓名
        Map<String, String> personKeyToName = new HashMap<>();
        Map<String, String> personKeyToEmployeeNo = new HashMap<>();
        Map<String, String> personKeyToDept = new HashMap<>();
        Map<String, String> personKeyToShiftClass = new HashMap<>();
        Map<String, Integer> personKeyToRowIndex = new HashMap<>();
        Map<String, Map<LocalDate, boolean[]>> canAttend = buildCanAttend(slots, shiftMap, year, month, daysInMonth,
            personKeyToName, personKeyToEmployeeNo, personKeyToDept, personKeyToShiftClass, personKeyToRowIndex);

        // 集中排课：可上午可下午的归同一时段
        Map<LocalDate, Map<String, List<String>>> daySlotPersons = centralize(canAttend, year, month);

        // 理论/实操日 + 学时封顶 + 拆班 30-50（班额、课时封顶优先用系统设置 DB 配置）
        Map<String, Map<LocalDate, Map<String, String>>> personDayClass = new HashMap<>();
        Map<LocalDate, List<ClassAssignResult.ClassCount>> dailyStats = new HashMap<>();

        List<LocalDate> orderedDates = new ArrayList<>(daySlotPersons.keySet());
        Collections.sort(orderedDates);
        for (LocalDate d : orderedDates) {
            boolean isTheory = theoryDays.contains(d);
            Map<String, List<String>> slotToPersons = daySlotPersons.get(d);
            for (String slotName : Arrays.asList("上午", "下午")) {
                List<String> persons = slotToPersons.getOrDefault(slotName, Collections.emptyList());
                if (persons.isEmpty()) continue;
                List<String> filtered = filterByHours(persons, personDayClass, d, isTheory, theoryDays, practiceDays, cfg);
                List<List<String>> classes = splitClasses(filtered, cfg.getClassMin(), cfg.getClassMax());
                int idx = 1;
                for (List<String> cls : classes) {
                    String className = slotName + idx + "班";
                    for (String p : cls) {
                        personDayClass
                            .computeIfAbsent(p, k -> new HashMap<>())
                            .computeIfAbsent(d, k -> new HashMap<>())
                            .put(slotName, className);
                    }
                    dailyStats.computeIfAbsent(d, k -> new ArrayList<>()).add(
                        new ClassAssignResult.ClassCount(slotName, idx, cls.size()));
                    idx++;
                }
            }
        }

        Set<String> assigned = personDayClass.keySet();
        Set<String> allPersons = canAttend.keySet();
        List<String> unassigned = allPersons.stream().filter(p -> !assigned.contains(p)).sorted().collect(Collectors.toList());
        Map<String, String> unassignedReasons = buildUnassignedReasons(slots, unassigned, shiftMap);

        ClassAssignResult result = new ClassAssignResult();
        result.setFactoryId(factoryId);
        result.setYear(year);
        result.setMonth(month);
        result.setTheoryDays(theoryDays);
        result.setPracticeDays(practiceDays);
        result.setPersonDayClass(personDayClass);
        result.setDailyStats(dailyStats);
        result.setUnassignedPersons(unassigned);
        result.setUnassignedReasons(unassignedReasons);
        result.setPersonKeyToName(personKeyToName);
        result.setPersonKeyToEmployeeNo(personKeyToEmployeeNo);
        result.setPersonKeyToDept(personKeyToDept);
        result.setPersonKeyToShiftClass(personKeyToShiftClass);
        result.setPersonKeyToRowIndex(personKeyToRowIndex);
        return result;
    }

    public TeacherScheduleResult runTeacherSchedule(ClassAssignResult classResult) {
        return runTeacherSchedule(
            classResult,
            ShiftAssignDefaults.theoryTeachers(),
            ShiftAssignDefaults.practiceTeachers());
    }

    public TeacherScheduleResult runTeacherSchedule(
            ClassAssignResult classResult,
            List<String> theoryTeachers,
            List<String> practiceTeachers) {
        if (theoryTeachers == null || theoryTeachers.isEmpty()) theoryTeachers = Arrays.asList("老师A", "老师B");
        if (practiceTeachers == null || practiceTeachers.isEmpty()) practiceTeachers = new ArrayList<>(theoryTeachers);

        Set<String> allTeachers = new LinkedHashSet<>();
        allTeachers.addAll(theoryTeachers);
        allTeachers.addAll(practiceTeachers);
        Map<String, Map<LocalDate, Map<String, String>>> teacherDaySchedule = new HashMap<>();
        for (String t : allTeachers) {
            teacherDaySchedule.put(t, new HashMap<>());
        }

        Map<LocalDate, List<ClassAssignResult.ClassCount>> stats = classResult.getDailyStats();
        List<String> theoryList = new ArrayList<>(theoryTeachers);
        List<String> practiceList = new ArrayList<>(practiceTeachers);
        int theoryIdx = 0;
        int practiceIdx = 0;

        // 理论老师：理论日排课，可周一到周日尽快排（理论日已含周末）
        for (LocalDate d : classResult.getTheoryDays()) {
            List<ClassAssignResult.ClassCount> counts = stats.getOrDefault(d, Collections.emptyList());
            for (ClassAssignResult.ClassCount c : counts) {
                String slotKey = c.getSlot().startsWith("上午") ? "上午" : "下午";
                String desc = "理论-" + c.getSlot() + c.getClassIndex() + "班";
                String t1 = theoryList.get(theoryIdx % theoryList.size());
                String t2 = theoryList.get((theoryIdx + 1) % theoryList.size());
                appendDesc(teacherDaySchedule, t1, d, slotKey, desc);
                appendDesc(teacherDaySchedule, t2, d, slotKey, desc);
                theoryIdx += 2;
            }
        }
        // 实操老师：实操日排课，周末不上课、尽量周二三四五（实操日已排除周末，且周三优先）
        for (LocalDate d : classResult.getPracticeDays()) {
            List<ClassAssignResult.ClassCount> counts = stats.getOrDefault(d, Collections.emptyList());
            for (ClassAssignResult.ClassCount c : counts) {
                String slotKey = c.getSlot().startsWith("上午") ? "上午" : "下午";
                String desc = "实操-" + c.getSlot() + c.getClassIndex() + "班";
                String t1 = practiceList.get(practiceIdx % practiceList.size());
                String t2 = practiceList.get((practiceIdx + 1) % practiceList.size());
                appendDesc(teacherDaySchedule, t1, d, slotKey, desc);
                appendDesc(teacherDaySchedule, t2, d, slotKey, desc);
                practiceIdx += 2;
            }
        }

        TeacherScheduleResult result = new TeacherScheduleResult();
        result.setFactoryId(classResult.getFactoryId());
        result.setYear(classResult.getYear());
        result.setMonth(classResult.getMonth());
        result.setTeacherDaySchedule(teacherDaySchedule);
        return result;
    }

    /** 同一老师同一日期同一时段可能带多班，用「、」拼接 */
    private void appendDesc(Map<String, Map<LocalDate, Map<String, String>>> teacherDaySchedule,
                            String teacher, LocalDate d, String slotKey, String desc) {
        Map<String, String> slotMap = teacherDaySchedule.get(teacher).computeIfAbsent(d, k -> new HashMap<>());
        slotMap.merge(slotKey, desc, (old, add) -> {
            if (old == null || old.isEmpty()) return add;
            // 避免重复追加同一班（常见于老师列表只有 1 人时，t1==t2）
            String[] parts = old.split("、");
            for (String p : parts) {
                if (p != null && p.trim().equals(add)) return old;
            }
            return old + "、" + add;
        });
    }

    private List<LocalDate> theoryDays(int year, int month) {
        List<LocalDate> out = new ArrayList<>();
        LocalDate start = LocalDate.of(year, month, 1);
        int days = start.lengthOfMonth();
        List<LocalDate> weekends = new ArrayList<>();
        List<LocalDate> weekdays = new ArrayList<>();
        for (int i = 1; i <= days; i++) {
            LocalDate d = LocalDate.of(year, month, i);
            int w = d.getDayOfWeek().getValue();
            if (w == 6 || w == 7) weekends.add(d);
            else weekdays.add(d);
        }
        out.addAll(weekends);
        out.addAll(weekdays);
        return out.stream().limit(12).collect(Collectors.toList());
    }

    private List<LocalDate> practiceDays(int year, int month, List<LocalDate> theoryDays) {
        Set<LocalDate> theorySet = new HashSet<>(theoryDays);
        List<LocalDate> out = new ArrayList<>();
        LocalDate start = LocalDate.of(year, month, 1);
        int days = start.lengthOfMonth();
        List<LocalDate> wed = new ArrayList<>(), other = new ArrayList<>();
        for (int i = 1; i <= days; i++) {
            LocalDate d = LocalDate.of(year, month, i);
            if (theorySet.contains(d)) continue;
            int w = d.getDayOfWeek().getValue();
            if (w == 6 || w == 7) continue;
            if (w == 3) wed.add(d);
            else other.add(d);
        }
        out.addAll(wed);
        out.addAll(other);
        return out;
    }

    private Map<String, Map<LocalDate, boolean[]>> buildCanAttend(List<PersonDaySlot> slots,
                                                                   Map<String, ShiftTimeDef> shiftMap,
                                                                   int year, int month, int daysInMonth,
                                                                   Map<String, String> personKeyToName,
                                                                   Map<String, String> personKeyToEmployeeNo,
                                                                   Map<String, String> personKeyToDept,
                                                                   Map<String, String> personKeyToShiftClass,
                                                                   Map<String, Integer> personKeyToRowIndex) {
        Map<String, Map<LocalDate, boolean[]>> can = new HashMap<>();
        // 次日限制：key=personKey_yyyy-MM-dd, mask: 1=AM, 2=PM, 3=ALL
        Map<String, Integer> nextDayBlocks = new HashMap<>();
        // 某人某日只要出现过夜班，则当天上午/下午都不可排（避免同一天多列时白班 slot 覆盖夜班）
        Map<LocalDate, Set<String>> dayPersonNight = new HashMap<>();
        for (PersonDaySlot s : slots) {
            if (s.getDay() < 1 || s.getDay() > daysInMonth) continue;
            String name = s.getName() != null ? s.getName().trim() : "";
            String employeeNo = s.getEmployeeNo();
            if (employeeNo != null) employeeNo = employeeNo.trim();
            if (employeeNo != null && employeeNo.isEmpty()) employeeNo = null;
            String personKey = (employeeNo != null && !employeeNo.isEmpty()) ? employeeNo : name;
            if (personKey.isEmpty()) continue;
            if (ShiftTimeParser.isNightShift(s.getShiftName())) {
                LocalDate d = LocalDate.of(year, month, s.getDay());
                dayPersonNight.computeIfAbsent(d, k -> new HashSet<>()).add(personKey);
            }
        }

        for (PersonDaySlot s : slots) {
            if (s.getDay() < 1 || s.getDay() > daysInMonth) continue; // 超出当月天数（如 2 月 30 日）忽略，不跨月
            String name = s.getName() != null ? s.getName().trim() : "";
            String employeeNo = s.getEmployeeNo();
            if (employeeNo != null) employeeNo = employeeNo.trim();
            if (employeeNo != null && employeeNo.isEmpty()) employeeNo = null;
            String personKey = (employeeNo != null && !employeeNo.isEmpty()) ? employeeNo : name;
            if (personKey.isEmpty()) continue;

            if (!can.containsKey(personKey)) {
                can.put(personKey, new HashMap<>());
                personKeyToName.put(personKey, name);
                if (employeeNo != null) personKeyToEmployeeNo.put(personKey, employeeNo);
                if (s.getDept() != null) personKeyToDept.put(personKey, s.getDept().trim());
                if (s.getShiftClass() != null && !s.getShiftClass().trim().isEmpty()) personKeyToShiftClass.put(personKey, s.getShiftClass().trim());
                if (s.getRowIndex() != null) {
                    Integer old = personKeyToRowIndex.get(personKey);
                    if (old == null || s.getRowIndex() < old) {
                        personKeyToRowIndex.put(personKey, s.getRowIndex());
                    }
                }
            } else if (s.getRowIndex() != null) {
                Integer old = personKeyToRowIndex.get(personKey);
                if (old == null || s.getRowIndex() < old) {
                    personKeyToRowIndex.put(personKey, s.getRowIndex());
                }
            }
            LocalDate d = LocalDate.of(year, month, s.getDay());
            boolean[] arr = can.get(personKey).computeIfAbsent(d, k -> new boolean[2]);

            if (ShiftTimeParser.isRest(s.getShiftName())) {
                arr[s.isMorning() ? 0 : 1] = false;
                continue;
            }
            String dayKey = personKey + "_" + d;
            int blockMask = nextDayBlocks.getOrDefault(dayKey, 0);
            if (blockMask == 3) { // 全天禁排
                arr[0] = false;
                arr[1] = false;
                continue;
            }
            if (s.isMorning() && (blockMask & 1) != 0) { // 仅上午禁排
                arr[0] = false;
                continue;
            }
            if (!s.isMorning() && (blockMask & 2) != 0) { // 仅下午禁排
                arr[1] = false;
                continue;
            }

            // 夜班/晚班：当天上午、下午均不可排课，且次日不排；不依赖班次时间表配置，避免误配「可排上午」导致夜班被排上午课
            if (ShiftTimeParser.isNightShift(s.getShiftName())) {
                arr[0] = false;
                arr[1] = false;
                String k = personKey + "_" + d.plusDays(1);
                nextDayBlocks.merge(k, 3, (oldV, addV) -> oldV | addV);
                continue;
            }

            ShiftTimeDef def = shiftMap != null ? shiftMap.get(normalizeShiftName(s.getShiftName())) : null;
            if (def != null && def.getStartTime() != null) {
                // 可拓展：优先使用 allowMorning/allowAfternoon 配置；未配置则默认允许（上班可抽时间上课）
                boolean morning = def.getAllowMorning() == null ? true : Boolean.TRUE.equals(def.getAllowMorning());
                boolean afternoon = def.getAllowAfternoon() == null ? true : Boolean.TRUE.equals(def.getAllowAfternoon());
                arr[0] = s.isMorning() ? morning : arr[0];
                arr[1] = !s.isMorning() ? afternoon : arr[1];
                int mask = def.getNextDayBlock() == null ? (def.isCrossDay() ? 3 : 0) : def.getNextDayBlock();
                if (mask != 0) {
                    String k = personKey + "_" + d.plusDays(1);
                    nextDayBlocks.merge(k, mask, (oldV, addV) -> oldV | addV);
                }
            } else {
                String normShift = normalizeShiftName(s.getShiftName());
                boolean[] rule = ShiftTimeParser.DEFAULT_SHIFT_RULES.getOrDefault(normShift, new boolean[]{true, true});
                arr[0] = s.isMorning() ? rule[0] : arr[0];
                arr[1] = !s.isMorning() ? rule[1] : arr[1];
            }
        }
        // 某人某日只要出现过夜班，当天上午/下午一律不可排（避免同一天多列时白班覆盖夜班）
        for (Map.Entry<LocalDate, Set<String>> e : dayPersonNight.entrySet()) {
            LocalDate d = e.getKey();
            Set<String> personKeys = e.getValue();
            for (String personKey : personKeys) {
                Map<LocalDate, boolean[]> byDay = can.get(personKey);
                if (byDay != null) {
                    boolean[] arr = byDay.get(d);
                    if (arr != null) {
                        arr[0] = false;
                        arr[1] = false;
                    }
                }
            }
        }
        return can;
    }

    private String normalizeShiftName(String name) {
        if (name == null) return "";
        String s = name.trim();
        // 统一常见分隔符与空白，便于「三班两倒-中班 / 三班两倒 中班 / 三班两倒中班」一致匹配
        s = s.replace("－", "-").replace("—", "-").replace("–", "-");
        s = s.replaceAll("\\s+", "");
        // 先做特例，再做通用去分隔符
        if (s.contains("四班两倒-")) s = s.replace("四班两倒-", "四班两倒");
        // 去掉连接符/下划线，避免同一班次多写法导致匹配不到 DB 配置
        s = s.replace("-", "").replace("_", "");
        return s;
    }

    /**
     * 为未排入任何班的人员生成原因说明，便于排查（全月休息、班次未配置、无重叠/夜班次日等）。
     */
    private Map<String, String> buildUnassignedReasons(List<PersonDaySlot> slots,
                                                       List<String> unassignedPersonKeys,
                                                       Map<String, ShiftTimeDef> shiftMap) {
        Map<String, String> reasons = new HashMap<>();
        if (unassignedPersonKeys == null || unassignedPersonKeys.isEmpty()) return reasons;
        Set<String> unassignedSet = new HashSet<>(unassignedPersonKeys);
        // 按人员唯一键分组其排班记录
        Map<String, List<PersonDaySlot>> slotsByPerson = new HashMap<>();
        for (PersonDaySlot s : slots) {
            String name = s.getName() != null ? s.getName().trim() : "";
            String employeeNo = s.getEmployeeNo();
            if (employeeNo != null) employeeNo = employeeNo.trim();
            if (employeeNo != null && employeeNo.isEmpty()) employeeNo = null;
            String personKey = (employeeNo != null && !employeeNo.isEmpty()) ? employeeNo : name;
            if (personKey.isEmpty() || !unassignedSet.contains(personKey)) continue;
            slotsByPerson.computeIfAbsent(personKey, k -> new ArrayList<>()).add(s);
        }
        Set<String> knownShifts = new HashSet<>();
        if (shiftMap != null) for (String k : shiftMap.keySet()) knownShifts.add(normalizeShiftName(k));
        knownShifts.addAll(ShiftTimeParser.DEFAULT_SHIFT_RULES.keySet());

        for (String personKey : unassignedPersonKeys) {
            List<PersonDaySlot> personSlots = slotsByPerson.getOrDefault(personKey, Collections.emptyList());
            boolean allRest = true;
            boolean hasUnknownShift = false;
            for (PersonDaySlot s : personSlots) {
                String sn = s.getShiftName();
                if (!ShiftTimeParser.isRest(sn)) {
                    allRest = false;
                    String norm = normalizeShiftName(sn);
                    if (!knownShifts.contains(norm)) hasUnknownShift = true;
                }
            }
            if (allRest) reasons.put(personKey, "全月为休息");
            else if (hasUnknownShift) reasons.put(personKey, "排班表班次未在班次时间中配置，该时段不可排课");
            else reasons.put(personKey, "全月班次与上课时间无重叠或夜班次日不排");
        }
        return reasons;
    }

    private Map<LocalDate, Map<String, List<String>>> centralize(Map<String, Map<LocalDate, boolean[]>> canAttend,
                                                                 int year, int month) {
        Map<LocalDate, Map<String, List<String>>> daySlot = new HashMap<>();
        for (String person : canAttend.keySet()) {
            Map<LocalDate, boolean[]> byDay = canAttend.get(person);
            for (LocalDate d : byDay.keySet()) {
                if (d.getYear() != year || d.getMonthValue() != month) continue; // 仅处理所选月份，不跨月
                boolean[] arr = byDay.get(d);
                boolean am = arr[0];
                boolean pm = arr[1];
                if (am) daySlot.computeIfAbsent(d, k -> new HashMap<>()).computeIfAbsent("上午", k -> new ArrayList<>()).add(person);
                if (pm) daySlot.computeIfAbsent(d, k -> new HashMap<>()).computeIfAbsent("下午", k -> new ArrayList<>()).add(person);
            }
        }
        // 业务规则：同一人同一天尽量排两次（上午 + 下午）。因此这里不做“二选一”削减，保留可排的两个时段。
        return daySlot;
    }

    private List<String> filterByHours(List<String> persons, Map<String, Map<LocalDate, Map<String, String>>> personDayClass,
                                        LocalDate d, boolean isTheory, List<LocalDate> theoryDays, List<LocalDate> practiceDays,
                                        EffectiveSchedulingConfig cfg) {
        int per = Math.max(1, cfg.getHoursPerSession());
        double theoryRate = cfg.getTheoryCompletionRate() > 0 ? cfg.getTheoryCompletionRate() : 1.0;
        double practiceRate = cfg.getPracticeCompletionRate() > 0 ? cfg.getPracticeCompletionRate() : 1.0;
        int theoryTargetHours = (int) Math.round(cfg.getTheoryHoursTotal() * theoryRate);
        int practiceTargetHours = (int) Math.round(cfg.getPracticeHoursTotal() * practiceRate);
        int theorySessions = Math.max(1, (theoryTargetHours + per - 1) / per);
        int practiceSessions = Math.max(1, (practiceTargetHours + per - 1) / per);
        List<String> out = new ArrayList<>();
        for (String p : persons) {
            Map<LocalDate, Map<String, String>> byDay = personDayClass.getOrDefault(p, Collections.emptyMap());
            long theoryCount = byDay.entrySet().stream()
                .filter(e -> theoryDays.contains(e.getKey()))
                .mapToLong(e -> e.getValue() != null ? e.getValue().size() : 0)
                .sum();
            long practiceCount = byDay.entrySet().stream()
                .filter(e -> practiceDays.contains(e.getKey()))
                .mapToLong(e -> e.getValue() != null ? e.getValue().size() : 0)
                .sum();
            if (isTheory && theoryCount >= theorySessions) continue;
            if (!isTheory && practiceCount >= practiceSessions) continue;
            out.add(p);
        }
        return out;
    }

    private List<List<String>> splitClasses(List<String> persons, int min, int max) {
        List<List<String>> result = new ArrayList<>();
        int n = persons.size();
        if (n == 0) return result;
        if (min > max) { int t = min; min = max; max = t; } // 防御：班额上下限反写时交换

        // 业务约束：接近 50 人时不再拆班
        // 场景：班额 30-50，若某时段人数在 [min, max + 5] 区间（例如 30~55 人），
        // 则宁可一个大班（约 50 人），也不拆成两个 25 人小班，避免额外成本。
        if (n >= min && n <= max + 5) {
            result.add(new ArrayList<>(persons));
            return result;
        }

        int classCount = (n + max - 1) / max;
        if (classCount * min > n) classCount = (n + min - 1) / min;
        int perClass = n / classCount;
        int rem = n % classCount;
        int idx = 0;
        for (int i = 0; i < classCount; i++) {
            int size = perClass + (i < rem ? 1 : 0);
            result.add(new ArrayList<>(persons.subList(idx, idx + size)));
            idx += size;
        }
        return result;
    }
}
