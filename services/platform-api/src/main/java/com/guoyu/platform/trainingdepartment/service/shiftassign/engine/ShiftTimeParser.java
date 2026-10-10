package com.guoyu.platform.trainingdepartment.service.shiftassign.engine;

import com.guoyu.platform.trainingdepartment.service.shiftassign.domain.ShiftTimeDef;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 班次时间解析：支持 "6：00~11:30"、"20:00~8:00"、"上午8:00~12：00下午14:30~18:00"
 */
public class ShiftTimeParser {

    private static final Pattern SINGLE = Pattern.compile("([0-2]?[0-9])[：:]([0-5][0-9])\\s*[~～-]\\s*([0-2]?[0-9])[：:]([0-5][0-9])");
    private static final Pattern ADMIN = Pattern.compile("上午\\s*([0-2]?[0-9])[：:]([0-5][0-9])[~～-]([0-2]?[0-9])[：:]([0-5][0-9])下午\\s*([0-2]?[0-9])[：:]([0-5][0-9])[~～-]([0-2]?[0-9])[：:]([0-5][0-9])");

    /** 班次别名：排班表常见简写 → 规范名，减少未识别（与 DEFAULT_SHIFT_RULES / 班次时间 配置一致） */
    private static final Map<String, String> SHIFT_ALIAS = new HashMap<>();
    static {
        SHIFT_ALIAS.put("白", "白班");
        SHIFT_ALIAS.put("中", "中班");
        SHIFT_ALIAS.put("夜", "夜班");
        SHIFT_ALIAS.put("晚", "晚班");
        SHIFT_ALIAS.put("长白", "长白班");
        SHIFT_ALIAS.put("行政", "行政班");
        // 0 在业务中约定为「夜班」：表示上夜班，白天需要休息，不排课
        SHIFT_ALIAS.put("0", "夜班");
        // 已为规范名的可不再映射
    }

    /**
     * 将排班表中的简写解析为规范班次名（用于与班次时间/默认规则匹配）。
     * 若存在别名则返回规范名，否则返回 null（调用方保留原值）。
     */
    public static String resolveShiftAlias(String raw) {
        if (raw == null) return null;
        String norm = normalizeShiftNameForLookup(raw);
        if (norm.isEmpty()) return null;
        return SHIFT_ALIAS.get(norm);
    }

    /** 默认班次规则：无班次时间表时使用（可排上午/下午）；与前端「班次时间」常用模板一致 */
    public static final Map<String, boolean[]> DEFAULT_SHIFT_RULES = new HashMap<>();
    static {
        // 单班/通用
        DEFAULT_SHIFT_RULES.put("白", new boolean[]{true, true});
        DEFAULT_SHIFT_RULES.put("白班", new boolean[]{true, true});
        DEFAULT_SHIFT_RULES.put("长白班", new boolean[]{true, true});
        DEFAULT_SHIFT_RULES.put("行政班", new boolean[]{true, true});
        DEFAULT_SHIFT_RULES.put("视为不倒班", new boolean[]{true, true});
        DEFAULT_SHIFT_RULES.put("分厂后勤", new boolean[]{true, true});
        DEFAULT_SHIFT_RULES.put("电积后勤", new boolean[]{true, true});
        DEFAULT_SHIFT_RULES.put("中", new boolean[]{false, true});
        DEFAULT_SHIFT_RULES.put("中班", new boolean[]{false, true});
        // 0 表示夜班：当天与次日均不排课（次日禁排由 buildCanAttend + isNightShift 统一处理）
        DEFAULT_SHIFT_RULES.put("0", new boolean[]{false, false});
        // 休息：当天不排课，但不触发「次日禁排」
        DEFAULT_SHIFT_RULES.put("休", new boolean[]{false, false});
        DEFAULT_SHIFT_RULES.put("", new boolean[]{false, false});
        DEFAULT_SHIFT_RULES.put("夜", new boolean[]{false, false});
        DEFAULT_SHIFT_RULES.put("夜班", new boolean[]{false, false});
        DEFAULT_SHIFT_RULES.put("晚班", new boolean[]{false, false});
        // 两班倒
        DEFAULT_SHIFT_RULES.put("两班倒-白班", new boolean[]{true, true});
        DEFAULT_SHIFT_RULES.put("两班倒-夜班", new boolean[]{false, false});
        // 三班两倒/三班倒（早班可上午下午、中班仅下午、夜班次日不排）
        DEFAULT_SHIFT_RULES.put("三班两倒-早班", new boolean[]{true, true});
        DEFAULT_SHIFT_RULES.put("三班两倒-中班", new boolean[]{false, true});
        DEFAULT_SHIFT_RULES.put("三班两倒-夜班", new boolean[]{false, false});
        // 四班两倒（规范化后 A=白/早、B=夜；lookup 用 normalizeShiftName 得到 四班两倒A/四班两倒B）
        DEFAULT_SHIFT_RULES.put("四班两倒A", new boolean[]{true, true});
        DEFAULT_SHIFT_RULES.put("四班两倒B", new boolean[]{false, false});
        DEFAULT_SHIFT_RULES.put("四班两倒早班", new boolean[]{true, true});
        DEFAULT_SHIFT_RULES.put("四班两倒夜班", new boolean[]{false, false});
        // 四班三运转
        DEFAULT_SHIFT_RULES.put("四班三运转-早", new boolean[]{true, true});
        DEFAULT_SHIFT_RULES.put("四班三运转-中", new boolean[]{false, true});
        DEFAULT_SHIFT_RULES.put("四班三运转-夜", new boolean[]{false, false});
        // 其他（电积、晚班等）
        DEFAULT_SHIFT_RULES.put("电积早班", new boolean[]{true, true});
        DEFAULT_SHIFT_RULES.put("电积中班", new boolean[]{false, true});
    }

    /**
     * 解析单行班次时间，返回一个或多个定义（行政班拆成上午段+下午段，合并为一个“可上午可下午”的虚拟班次）
     */
    public ShiftTimeDef parse(String shiftName, String timeStr) {
        if (shiftName == null) shiftName = "";
        String raw = (timeStr == null ? "" : timeStr).trim();
        if (raw.isEmpty() || "休".equals(shiftName) || "0".equals(shiftName)) {
            return ShiftTimeDef.rest();
        }

        // 行政班：上午8:00~12：00下午14:30~18:00 → 可排上午、可排下午（用上午段和下午段分别与上课时段重叠即可）
        Matcher am = ADMIN.matcher(raw);
        if (am.find()) {
            ShiftTimeDef d = new ShiftTimeDef();
            d.setShiftName(shiftName);
            d.setStartTime(norm(am.group(1), am.group(2)));
            d.setEndTime(norm(am.group(3), am.group(4)));
            d.setCrossDay(false);
            return d; // 行政班：只取上午段 08:00-12:00 已覆盖上午课；下午段 14:30-18:00 覆盖下午课。这里简化为一个时段 08:00-18:00 即可同时可上午可下午
        }

        Matcher m = SINGLE.matcher(raw);
        if (m.find()) {
            int sH = Integer.parseInt(m.group(1));
            int sM = Integer.parseInt(m.group(2));
            int eH = Integer.parseInt(m.group(3));
            int eM = Integer.parseInt(m.group(4));
            String start = norm(m.group(1), m.group(2));
            String end = norm(m.group(3), m.group(4));
            ShiftTimeDef d = new ShiftTimeDef();
            d.setShiftName(shiftName);
            d.setStartTime(start);
            d.setEndTime(end);
            d.setCrossDay(eH < sH || (eH == sH && eM < sM));
            return d;
        }

        // 行政班简写：整段 08:00-18:00
        if (raw.contains("上午") && raw.contains("下午")) {
            ShiftTimeDef d = new ShiftTimeDef();
            d.setShiftName(shiftName);
            d.setStartTime("08:00");
            d.setEndTime("18:00");
            d.setCrossDay(false);
            return d;
        }
        return ShiftTimeDef.rest();
    }

    private static String norm(String h, String m) {
        return String.format("%02d:%02d", Integer.parseInt(h), Integer.parseInt(m));
    }

    /** 判断班次时间段与上午课(08:00-12:00)、下午课(14:00-18:00)是否重叠 */
    public static boolean overlap(String start, String end, boolean crossDay, boolean isMorningSlot) {
        if (start == null || end == null) return false;
        int[] slot = isMorningSlot ? new int[]{8 * 60, 12 * 60} : new int[]{14 * 60, 18 * 60};
        int s = toMin(start);
        int e = toMin(end);
        if (crossDay) {
            if (e < s) e += 24 * 60;
            return (s <= slot[1] && e >= slot[0]) || (s + 24 * 60 <= slot[1] && e + 24 * 60 >= slot[0]);
        }
        return s < slot[1] && e > slot[0];
    }

    private static int toMin(String t) {
        String[] p = t.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }

    /** 是否休息/不排（不含 0；0 已按夜班处理） */
    public static boolean isRest(String shiftName) {
        if (shiftName == null) return true;
        String s = shiftName.trim();
        return s.isEmpty() || "休".equals(s);
    }

    /** 是否夜班/晚班（次日不排 R3） */
    public static boolean isNightShift(String shiftName) {
        if (shiftName == null) return false;
        String s = shiftName.trim();
        return s.contains("夜") || s.contains("晚") || "夜".equals(s) || "夜班".equals(s) || "晚班".equals(s);
    }

    /** 班次名称规范化用于查找（与 ScheduleAlgorithmService.normalizeShiftName 一致，便于 DB 配置与排班表名称匹配） */
    public static String normalizeShiftNameForLookup(String name) {
        if (name == null) return "";
        String s = name.trim();
        s = s.replace("－", "-").replace("—", "-").replace("–", "-");
        s = s.replaceAll("\\s+", "");
        if (s.contains("四班两倒-")) s = s.replace("四班两倒-", "四班两倒");
        // 去掉连接符/下划线，统一「三班两倒-中班」「三班两倒 中班」「三班两倒中班」等写法
        s = s.replace("-", "").replace("_", "");
        return s;
    }
}
