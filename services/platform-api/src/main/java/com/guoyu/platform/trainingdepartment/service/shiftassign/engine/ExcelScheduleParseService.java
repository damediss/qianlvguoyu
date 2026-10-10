package com.guoyu.platform.trainingdepartment.service.shiftassign.engine;

import com.guoyu.platform.trainingdepartment.service.shiftassign.ShiftAssignDefaults;
import com.guoyu.platform.trainingdepartment.service.shiftassign.domain.PersonDaySlot;
import com.guoyu.platform.trainingdepartment.service.shiftassign.domain.ScheduleParseResult;
import com.guoyu.platform.trainingdepartment.service.shiftassign.domain.ShiftTimeDef;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Excel 排班表解析（按天一格，不分上午/下午）。
 * 通用规则（适配多种表结构）：
 * 1）先找「姓名」所在行作为表头行，数据从该行下一行开始；
 * 2）日期列从表头行及表头上一行中识别（支持 1、2、3 或 1日、2日 或 一、二、三 等）；
 * 3）工号/身份证号/工段/班别从表头行识别，没有则不用；
 * 4）班次内容与班次时间配置匹配，未识别会提示。
 */
@Service
public class ExcelScheduleParseService {

    /** 日期：支持 “22日 / 22号 / 3/22 / 2025-03-22”等；不关心上午/下午 */
    private static final Pattern DAY_COL = Pattern.compile("(?:(\\d{4})[-/.])?(\\d{1,2})[-/.](\\d{1,2})|(?:(\\d+))\\s*[日号]?");
    /** 额外支持 “3月22日 / 03月22号 / 3月22” 等中文月份表头 */
    private static final Pattern CN_MONTH_DAY = Pattern.compile("(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*(?:[日号])?");
    /** 中文数字日 一～三十一（用于表头 一、二、三 等） */
    private static final String[] CN_DAYS = {"一","二","三","四","五","六","七","八","九","十",
        "十一","十二","十三","十四","十五","十六","十七","十八","十九","二十",
        "廿一","廿二","廿三","廿四","廿五","廿六","廿七","廿八","廿九","三十","三十一"};

    /** 日期列信息：列号、日、是否上午、是否明确写了上午/下午 */
    private static class DateCol {
        final int col;
        final int day;
        final boolean morning;
        final boolean explicitSlot;
        DateCol(int col, int day, boolean morning, boolean explicitSlot) {
            this.col = col;
            this.day = day;
            this.morning = morning;
            this.explicitSlot = explicitSlot;
        }
    }
    private static final Set<String> NAME_HEADERS = new HashSet<>(Arrays.asList("姓名", "员工姓名"));
    private static final Set<String> EMPLOYEE_NO_HEADERS = new HashSet<>(Arrays.asList("工号", "员工工号", "编号", "身份证号"));
    /** 工段/部门列：取第一个匹配的列作为工段（部门） */
    private static final Set<String> SECTION_HEADERS = new HashSet<>(Arrays.asList("工段", "部门"));
    /** 班别列：取第一个匹配的列作为班别 */
    private static final Set<String> SHIFTCLASS_HEADERS = new HashSet<>(Arrays.asList("班别"));

    public ScheduleParseResult parseBytes(byte[] fileBytes, String originalFilename, String factoryId, int year, int month) throws Exception {
        return parseBytes(fileBytes, originalFilename, factoryId, year, month, null);
    }

    public ScheduleParseResult parseBytes(
            byte[] fileBytes,
            String originalFilename,
            String factoryId,
            int year,
            int month,
            Map<String, ShiftTimeDef> shiftTimeMapOverride) throws Exception {
        Map<String, ShiftTimeDef> shiftTimeMap = shiftTimeMapOverride;
        if (shiftTimeMap == null || shiftTimeMap.isEmpty()) {
            shiftTimeMap = ShiftAssignDefaults.defaultShiftTimeMap();
        }

        List<PersonDaySlot> slots = new ArrayList<>();
        List<String> failedExtractRows = new ArrayList<>();
        List<String> unrecognizedShiftList = new ArrayList<>();
        Set<String> seenUnrecognized = new HashSet<>(); // 用于去重：personKey_day

        // 已知班次名称（规范化）用于判断未识别班次
        Set<String> knownShiftNorm = new HashSet<>();
        for (String k : shiftTimeMap.keySet()) {
            knownShiftNorm.add(ShiftTimeParser.normalizeShiftNameForLookup(k));
        }
        // 默认规则中的班次也视为已知
        for (String k : ShiftTimeParser.DEFAULT_SHIFT_RULES.keySet()) {
            knownShiftNorm.add(ShiftTimeParser.normalizeShiftNameForLookup(k));
        }

        validateExcelFileHeader(fileBytes, originalFilename);

        try (InputStream is = new ByteArrayInputStream(fileBytes);
             Workbook wb = WorkbookFactory.create(is)) {
            // 1）通用识别：先找「姓名」所在行作为表头，再从该行及上一行收集日期列（支持数字与中文 一、二、三）
            Sheet sheet = null;
            int headerRow = -1;
            int nameCol = -1;
            int employeeNoCol = -1;
            int sectionCol = -1;
            int shiftClassCol = -1;
            List<DateCol> dateCols = new ArrayList<>();

            outer:
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet s = wb.getSheetAt(i);
                if (s == null) continue;
                int lastRow = s.getLastRowNum();
                for (int r = 0; r <= Math.min(50, lastRow); r++) {
                    Row nameRow = s.getRow(r);
                    if (nameRow == null) continue;
                    int nameColFound = -1, empCol = -1, secCol = -1, shiftCol = -1;
                    int maxCol = nameRow.getLastCellNum();
                    for (int col = 0; col < maxCol; col++) {
                        String raw = cellStr(nameRow.getCell(col));
                        if (raw == null) continue;
                        String t = raw.trim();
                        if (t.contains("姓名") || NAME_HEADERS.contains(t)) { nameColFound = col; break; }
                    }
                    if (nameColFound < 0) continue;
                    // 本行有姓名，再取工号/工段/班别
                    for (int col = 0; col < maxCol; col++) {
                        String raw = cellStr(nameRow.getCell(col));
                        if (raw == null) continue;
                        String t = raw.trim();
                        if (empCol < 0 && (t.contains("工号") || EMPLOYEE_NO_HEADERS.contains(t))) empCol = col;
                        else if (secCol < 0 && SECTION_HEADERS.contains(t)) secCol = col;
                        else if (shiftCol < 0 && SHIFTCLASS_HEADERS.contains(t)) shiftCol = col;
                    }
                    // 日期列：从本行及上方多行收集（常见：日期在姓名行上方 1～2 行，或与姓名同行）
                    // col -> [day, morningFlag(1/0), explicitSlot(1/0)]
                    Map<Integer, int[]> colToInfo = new LinkedHashMap<>();
                    for (int scan = r; scan >= Math.max(0, r - 5); scan--) {
                        Row row = s.getRow(scan);
                        if (row == null) continue;
                        int rowMaxCol = row.getLastCellNum();
                        for (int col = 0; col < rowMaxCol; col++) {
                            String h = cellStr(row.getCell(col));
                            if (h == null) continue;
                            String trim = h.trim();
                            Integer day = parseDayFromHeader(trim);
                            if (day == null) continue;
                            Boolean slot = parseSlotFromHeader(trim); // null=未注明
                            boolean morning = slot == null || slot;   // 未注明时默认上午（兼容“一天一格”）
                            boolean explicit = slot != null;
                            int[] existing = colToInfo.get(col);
                            if (existing == null) {
                                colToInfo.put(col, new int[]{day, morning ? 1 : 0, explicit ? 1 : 0});
                            } else {
                                // 若新值带明确上午/下午，而旧值不带，则用新值覆盖；否则保留先扫到的（靠上的行优先）
                                if (explicit && existing[2] == 0) colToInfo.put(col, new int[]{day, morning ? 1 : 0, 1});
                            }
                        }
                    }
                    if (colToInfo.isEmpty()) continue;
                    // 避免把“标题行”（第 0 行整段含“姓名”且同列解析出 1 个日期）当表头
                    if (r == 0 && colToInfo.size() == 1 && colToInfo.containsKey(nameColFound)) continue;
                    List<DateCol> tmpDateCols = new ArrayList<>();
                    for (Map.Entry<Integer, int[]> e : colToInfo.entrySet()) {
                        Integer col = e.getKey();
                        int[] info = e.getValue();
                        // 防止把姓名/工号/工段/班别等业务列误识别为日期列，导致“未识别班次”出现人名
                        if (col == nameColFound || col == empCol || col == secCol || col == shiftCol) continue;
                        tmpDateCols.add(new DateCol(col, info[0], info[1] == 1, info[2] == 1));
                    }
                    if (tmpDateCols.isEmpty()) continue;
                    sheet = s;
                    headerRow = r;
                    nameCol = nameColFound;
                    employeeNoCol = empCol;
                    sectionCol = secCol;
                    shiftClassCol = shiftCol;
                    dateCols = tmpDateCols;
                    break outer;
                }
            }

            if (sheet == null || headerRow < 0) {
                return buildResult(factoryId, year, month, shiftTimeMap, slots, failedExtractRows, new ArrayList<>());
            }

            int lastRow = sheet.getLastRowNum();

            // 数据区：从表头下一行开始。表头行取实际包含「姓名」的那一行，避免标题行带 1,2,3 时把表头当数据导致少读
            final int dataStartRow = headerRow + 1;
            // 结束行：取 getLastRowNum() 与「从最后一行向前扫到第一个姓名列非空的行」的较大值，避免空行导致提前结束
            int dataEndRow = lastRow;
            for (int rr = lastRow; rr >= dataStartRow && rr >= lastRow - 5000; rr--) {
                Row probe = sheet.getRow(rr);
                if (probe == null) continue;
                String nm = cellStr(probe.getCell(nameCol));
                if (nm != null && !nm.trim().isEmpty() && !"姓名".equals(nm.trim()) && !"员工姓名".equals(nm.trim())) {
                    dataEndRow = rr;
                    break;
                }
            }

            for (int r = dataStartRow; r <= dataEndRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String name = cellStr(row.getCell(nameCol));
                if (name == null || name.trim().isEmpty()) {
                    // 有数据行但姓名为空：记为提取失败
                    String firstCell = cellStr(row.getCell(0));
                    if (firstCell != null && !firstCell.trim().isEmpty()) {
                        failedExtractRows.add("第" + (r + 1) + "行：姓名为空");
                    }
                    continue;
                }
                name = name.trim();
                if ("姓名".equals(name) || "员工姓名".equals(name)) continue;
                if ("示例".equals(name)) continue;

                String employeeNo = employeeNoCol >= 0 ? cellStr(row.getCell(employeeNoCol)) : null;
                if (employeeNo != null) {
                    employeeNo = employeeNo.trim();
                    if (employeeNo.isEmpty()) employeeNo = null;
                }
                String personKey = (employeeNo != null ? employeeNo : name);

                String dept = sectionCol >= 0 ? cellStr(row.getCell(sectionCol)) : "";
                if (dept == null) dept = "";
                String shiftClass = shiftClassCol >= 0 ? cellStr(row.getCell(shiftClassCol)) : "";
                if (shiftClass == null) shiftClass = "";

                boolean hasAnyShift = false;
                // Excel 原表行号（从 1 开始），用于后续课表视图按原表顺序展示
                int rowIndex = r + 1;
                for (DateCol dc : dateCols) {
                    String shift = cellStr(row.getCell(dc.col));
                    if (shift == null || shift.trim().isEmpty()) continue;
                    hasAnyShift = true;
                    shift = shift.trim();
                    // 班次别名：如「白」→「白班」，便于与班次时间配置匹配，减少未识别
                    String resolved = ShiftTimeParser.resolveShiftAlias(shift);
                    String lookupShift = (resolved != null && !resolved.isEmpty()) ? resolved : shift;
                    // 未识别班次：留空仍参与人员列表，但记录到未识别名单供前端展示
                    String shiftNorm = ShiftTimeParser.normalizeShiftNameForLookup(lookupShift);
                    boolean recognized = "0".equals(lookupShift) || "休".equals(lookupShift) || "休息".equals(lookupShift)
                        || knownShiftNorm.contains(shiftNorm);

                    // 若表头未明确上午/下午（一天一格），视为当天可能安排上午+下午两次：生成两个 slot，后续算法可真正排出“下午班”
                    // 若表头明确上午/下午，则只生成对应时段，保持精确。
                    List<Boolean> slotFlags = dc.explicitSlot ? Collections.singletonList(dc.morning) : Arrays.asList(Boolean.TRUE, Boolean.FALSE);
                    for (Boolean isMorning : slotFlags) {
                        boolean m = Boolean.TRUE.equals(isMorning);
                        if (!recognized) {
                            String uk = personKey + "_" + dc.day + "_" + (m ? "AM" : "PM");
                            if (!seenUnrecognized.contains(uk)) {
                                seenUnrecognized.add(uk);
                                unrecognizedShiftList.add(name + (employeeNo != null ? " " + employeeNo : "") + " " + dc.day + "日" + (m ? "上午" : "下午") + " 班次「" + shift + "」未在班次时间中配置");
                            }
                        }
                        slots.add(PersonDaySlot.builder()
                                .factoryId(factoryId)
                                .name(name)
                                .employeeNo(employeeNo)
                                .dept(dept)
                                .shiftClass(shiftClass)
                                .day(dc.day)
                                .morning(m)
                                .shiftName(lookupShift)
                                .rowIndex(rowIndex)
                                .build());
                    }
                }

                // 某些表可能“人员行存在但整行排班为空”，此时仍应纳入人员集合，避免总人数不一致；
                // 这里用一个“休息(0)”占位 slot 让后续算法能统计到该人员（会作为未排入/全月休息）。
                if (!hasAnyShift) {
                    slots.add(PersonDaySlot.builder()
                            .factoryId(factoryId)
                            .name(name)
                            .employeeNo(employeeNo)
                            .dept(dept)
                            .shiftClass(shiftClass)
                            .day(1)
                            .morning(true)
                            .shiftName("0")
                            .rowIndex(rowIndex)
                            .build());
                }
            }
        }

        return buildResult(factoryId, year, month, shiftTimeMap, slots, failedExtractRows, unrecognizedShiftList);
    }

    /**
     * 校验是否为 POI 可读的 Excel：.xlsx（ZIP 头 PK）或 .xls（OLE2）。
     * 避免把 CSV、HTML、WPS 专有格式等误当 Excel 时出现难懂的 POI 英文报错。
     */
    private static void validateExcelFileHeader(byte[] data, String originalName) {
        if (data == null || data.length < 8) {
            throw new IllegalArgumentException(
                "文件过小或为空。请上传由 Excel 保存的 .xlsx 或 .xls 排班表。");
        }
        boolean zipLike = data[0] == 0x50 && data[1] == 0x4B;
        boolean ole2 = data[0] == (byte) 0xD0 && data[1] == (byte) 0xCF
            && data[2] == 0x11 && data[3] == (byte) 0xE0;
        if (zipLike || ole2) {
            return;
        }
        String hint = "当前文件不是标准 Excel 二进制格式。";
        if (originalName != null && originalName.toLowerCase(Locale.ROOT).endsWith(".et")) {
            hint += " WPS 表格请使用「另存为」选择「Excel 工作簿(.xlsx)」后再上传。";
        } else {
            hint += " 请勿将 CSV、网页或 WPS 专有格式仅改后缀为 .xlsx；请用 Microsoft Excel 或 WPS 另存为「.xlsx / .xls」。";
        }
        throw new IllegalArgumentException(hint);
    }

    private ScheduleParseResult buildResult(String factoryId, int year, int month, Map<String, ShiftTimeDef> shiftTimeMap, List<PersonDaySlot> slots, List<String> failedExtractRows, List<String> unrecognizedShiftList) {
        ScheduleParseResult result = new ScheduleParseResult();
        result.setFactoryId(factoryId);
        result.setYear(year);
        result.setMonth(month);
        result.setShiftTimeMap(shiftTimeMap);
        result.setPersonDaySlots(slots);
        result.setTeacherNames(Arrays.asList("老师A", "老师B"));
        result.setFailedExtractRows(failedExtractRows != null ? failedExtractRows : Collections.emptyList());
        result.setUnrecognizedShiftList(unrecognizedShiftList != null ? unrecognizedShiftList : Collections.emptyList());
        return result;
    }

    /**
     * 从表头单元格解析出日期 1-31。支持：数字 1、2、1日、2日，或中文 一、二、三、...、三十一。
     */
    private static Integer parseDayFromHeader(String trim) {
        if (trim == null || trim.isEmpty()) return null;
        String lower = trim.toLowerCase(Locale.ROOT).trim();

        // 先严格识别“X月Y日/号”
        Matcher m2 = CN_MONTH_DAY.matcher(lower);
        if (m2.matches()) {
            int day = Integer.parseInt(m2.group(2));
            if (day >= 1 && day <= 31) return day;
        }

        // 再识别纯“日”样式：1 / 1日 / 1号
        if (lower.matches("^\\d{1,2}(?:\\s*[日号])?$") ) {
            String digits = lower.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                int day = Integer.parseInt(digits);
                if (day >= 1 && day <= 31) return day;
            }
        }

        // 最后识别完整日期：yyyy-m-d 或 m-d
        Matcher m = DAY_COL.matcher(lower);
        if (m.matches()) {
            Integer day = null;
            if (m.group(3) != null) day = Integer.parseInt(m.group(3));
            else if (m.group(4) != null) day = Integer.parseInt(m.group(4));
            if (day != null && day >= 1 && day <= 31) return day;
        }

        for (int i = 0; i < CN_DAYS.length; i++) {
            if (CN_DAYS[i].equals(trim)) return i + 1;
        }
        return null;
    }

    /** 从表头判断上午/下午；null 表示未注明（一天一格） */
    private static Boolean parseSlotFromHeader(String trim) {
        if (trim == null) return null;
        String s = trim.replaceAll("\\s+", "");
        if (s.contains("上午")) return true;
        if (s.contains("下午")) return false;
        return null;
    }

    private int findHeaderRow(Sheet sheet, int lastRow) {
        for (int r = 0; r <= Math.min(30, lastRow); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < Math.min(10, row.getLastCellNum()); c++) {
                String v = cellStr(row.getCell(c));
                if (v != null && (v.contains("姓名") || NAME_HEADERS.contains(v.trim()))) return r;
            }
        }
        return -1;
    }

    private int findAfternoonHeaderRow(Sheet sheet, int start, int lastRow) {
        for (int r = start; r <= Math.min(start + 20, lastRow); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                String v = cellStr(row.getCell(c));
                if (v != null && v.contains("日下午")) return r;
            }
        }
        return -1;
    }

    private static String cellStr(Cell c) {
        if (c == null) return null;
        switch (c.getCellType()) {
            case STRING:
                return c.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(c)) return c.getLocalDateTimeCellValue().toLocalDate().toString();
                double n = c.getNumericCellValue();
                if (n == (long) n) return String.valueOf((long) n);
                return String.valueOf(n);
            case BOOLEAN:
                return String.valueOf(c.getBooleanCellValue());
            default:
                return null;
        }
    }
}
