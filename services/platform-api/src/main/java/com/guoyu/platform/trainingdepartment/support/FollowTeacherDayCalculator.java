package com.guoyu.platform.trainingdepartment.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * 跟班教师上课天数：1 课次 = 0.5 天；同一教师同一日最多计 2 课次。
 * 按「老师 × 授课形式」输出行；同日封顶跨形式生效。
 */
public final class FollowTeacherDayCalculator {

  public static final BigDecimal SESSION_UNIT_DAYS = new BigDecimal("0.5");
  public static final int MAX_SESSIONS_PER_DAY = 2;
  public static final String FORM_BLANK = "未填";
  public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

  private FollowTeacherDayCalculator() {}

  public record SessionInput(
      String scheduleDate, String startTime, String teacherName, String teachForm) {}

  /** 兼容旧构造：无时间与形式 */
  public static SessionInput session(String scheduleDate, String teacherName) {
    return new SessionInput(scheduleDate, null, teacherName, null);
  }

  public record TeacherFormDays(
      String teacherName,
      String teachForm,
      int taughtSessionCount,
      BigDecimal taughtDays,
      int plannedSessionCount,
      BigDecimal plannedDays,
      int overflowSessionCount) {}

  public record ClassTeacherDays(
      LocalDate asOfDate,
      BigDecimal sessionUnitDays,
      int maxSessionsPerDay,
      int assignedSessionCount,
      int unassignedSessionCount,
      int overflowSessionCount,
      BigDecimal taughtDaysSum,
      BigDecimal plannedDaysSum,
      List<TeacherFormDays> teachers) {}

  public static ClassTeacherDays compute(List<SessionInput> sessions) {
    return compute(sessions, LocalDate.now(ZONE));
  }

  public static ClassTeacherDays compute(List<SessionInput> sessions, LocalDate asOfDate) {
    LocalDate cutoff = asOfDate == null ? LocalDate.now(ZONE) : asOfDate;
    List<SessionInput> list = sessions == null ? List.of() : sessions;

    int assigned = 0;
    int unassigned = 0;
    // teacher -> date -> sessions that day
    Map<String, Map<String, List<SessionInput>>> byTeacherDate = new LinkedHashMap<>();

    for (SessionInput row : list) {
      String name = normalizeName(row.teacherName());
      if (!StringUtils.hasText(name)) {
        unassigned++;
        continue;
      }
      assigned++;
      String date = normalizeDate(row.scheduleDate());
      String dateKey = StringUtils.hasText(date) ? date : "";
      byTeacherDate
          .computeIfAbsent(name, key -> new LinkedHashMap<>())
          .computeIfAbsent(dateKey, key -> new ArrayList<>())
          .add(row);
    }

    // (teacher|form) -> counters
    Map<String, int[]> formCounters = new LinkedHashMap<>();
    // teacher -> overflow
    Map<String, Integer> teacherOverflow = new LinkedHashMap<>();

    for (Map.Entry<String, Map<String, List<SessionInput>>> te : byTeacherDate.entrySet()) {
      String teacher = te.getKey();
      int overflow = 0;
      for (Map.Entry<String, List<SessionInput>> de : te.getValue().entrySet()) {
        String date = de.getKey();
        List<SessionInput> daySessions = new ArrayList<>(de.getValue());
        daySessions.sort(
            Comparator.comparing((SessionInput s) -> normalizeTime(s.startTime()), Comparator.nullsLast(String::compareTo))
                .thenComparing(s -> normalizeTeachForm(s.teachForm())));

        int raw = daySessions.size();
        overflow += Math.max(0, raw - MAX_SESSIONS_PER_DAY);
        List<SessionInput> counted =
            daySessions.subList(0, Math.min(raw, MAX_SESSIONS_PER_DAY));

        boolean past = StringUtils.hasText(date) && isOnOrBefore(date, cutoff);
        for (SessionInput s : counted) {
          String form = normalizeTeachForm(s.teachForm());
          String key = teacher + "\0" + form;
          int[] c = formCounters.computeIfAbsent(key, k -> new int[2]); // [taught, planned]
          c[1]++;
          if (past) {
            c[0]++;
          }
        }
      }
      teacherOverflow.put(teacher, overflow);
    }

    List<TeacherFormDays> rows = new ArrayList<>();
    int overflowSum = 0;
    BigDecimal taughtSum = BigDecimal.ZERO;
    BigDecimal plannedSum = BigDecimal.ZERO;
    Map<String, Integer> overflowLeft = new LinkedHashMap<>(teacherOverflow);

    for (Map.Entry<String, int[]> e : formCounters.entrySet()) {
      String[] parts = e.getKey().split("\0", 2);
      String teacher = parts[0];
      String form = parts.length > 1 ? parts[1] : FORM_BLANK;
      int taughtSessions = e.getValue()[0];
      int plannedSessions = e.getValue()[1];
      int overflow = overflowLeft.getOrDefault(teacher, 0);
      // 超限记在该老师第一行形式，避免重复加总
      int rowOverflow = overflow;
      if (rowOverflow > 0) {
        overflowLeft.put(teacher, 0);
        overflowSum += rowOverflow;
      }
      BigDecimal taughtDays = daysOf(taughtSessions);
      BigDecimal plannedDays = daysOf(plannedSessions);
      taughtSum = taughtSum.add(taughtDays);
      plannedSum = plannedSum.add(plannedDays);
      rows.add(new TeacherFormDays(
          teacher, form, taughtSessions, taughtDays, plannedSessions, plannedDays, rowOverflow));
    }

    rows.sort(
        Comparator.comparing(TeacherFormDays::taughtDays)
            .reversed()
            .thenComparing(TeacherFormDays::teacherName)
            .thenComparing(TeacherFormDays::teachForm));

    return new ClassTeacherDays(
        cutoff,
        SESSION_UNIT_DAYS,
        MAX_SESSIONS_PER_DAY,
        assigned,
        unassigned,
        overflowSum,
        round1(taughtSum),
        round1(plannedSum),
        rows);
  }

  public static String normalizeName(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "";
    }
    return raw.trim().replace("\u3000", "").trim();
  }

  public static String normalizeTeachForm(String raw) {
    if (!StringUtils.hasText(raw)) {
      return FORM_BLANK;
    }
    String v = raw.trim().replace("\u3000", "");
    if (!StringUtils.hasText(v)) {
      return FORM_BLANK;
    }
    if (v.contains("理论")) {
      return "理论";
    }
    if (v.contains("实操")) {
      return "实操";
    }
    return v;
  }

  static String normalizeDate(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "";
    }
    String s = raw.trim();
    if (s.length() >= 10 && s.charAt(4) == '-' && s.charAt(7) == '-') {
      return s.substring(0, 10);
    }
    return s;
  }

  static String normalizeTime(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "";
    }
    String v = raw.trim();
    if (v.contains(" ")) {
      v = v.substring(v.lastIndexOf(' ') + 1);
    }
    if (v.matches("\\d{1,2}:\\d{2}(:\\d{2})?")) {
      String[] p = v.split(":");
      String hh = p[0].length() == 1 ? "0" + p[0] : p[0];
      return hh + ":" + p[1];
    }
    return v;
  }

  static boolean isOnOrBefore(String scheduleDate, LocalDate asOfDate) {
    if (!StringUtils.hasText(scheduleDate) || asOfDate == null) {
      return false;
    }
    try {
      return !LocalDate.parse(scheduleDate).isAfter(asOfDate);
    } catch (Exception e) {
      return false;
    }
  }

  private static BigDecimal daysOf(int sessionCount) {
    return round1(SESSION_UNIT_DAYS.multiply(BigDecimal.valueOf(sessionCount)));
  }

  private static BigDecimal round1(BigDecimal value) {
    if (value == null) {
      return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
    }
    return value.setScale(1, RoundingMode.HALF_UP);
  }
}
