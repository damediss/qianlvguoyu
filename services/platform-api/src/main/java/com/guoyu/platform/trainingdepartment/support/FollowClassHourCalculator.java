package com.guoyu.platform.trainingdepartment.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * 跟班学员课时口径：45 分钟 = 1 课时；课表起止时间优先，否则用 classHours（不再 ÷45）。
 * 已上课时按「是否旷课」：仅统计截止日及以前的课次；默认计入，仅考勤明确缺勤扣除。
 */
public final class FollowClassHourCalculator {

  public static final int MINUTE_UNIT = 45;
  public static final String STATUS_OK = "ok";
  public static final String STATUS_UNRECORDED = "unrecorded";
  public static final String STATUS_PARTIAL = "partial";
  public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

  private static final BigDecimal FORTY_FIVE = BigDecimal.valueOf(MINUTE_UNIT);

  private FollowClassHourCalculator() {}

  public record SessionInput(
      Long id, String scheduleDate, String startTime, String endTime, String classHours) {}

  public record AttendanceInput(Long peopleId, String attendanceDate, Long scheduleId, Integer status) {}

  public record StudentInput(Long peopleId, String name) {}

  public record ResolvedSession(Long id, String date, BigDecimal hours) {
    boolean convertible() {
      return hours != null;
    }
  }

  public record StudentHours(
      Long peopleId,
      String name,
      BigDecimal attendedHours,
      BigDecimal plannedHours,
      BigDecimal completionRate,
      int presentSessions,
      int absentSessions,
      String hoursStatus) {}

  public record ClassHours(
      BigDecimal plannedHours,
      BigDecimal plannedHoursFromExcel,
      int unconvertibleSessions,
      int recordedStudentCount,
      int unrecordedStudentCount,
      BigDecimal attendedHoursSum,
      BigDecimal attendedHoursAvg,
      BigDecimal hoursCompletionAvg,
      List<StudentHours> students) {}

  public static ClassHours compute(
      List<SessionInput> sessions, List<AttendanceInput> attendances, List<StudentInput> students) {
    return compute(sessions, attendances, students, LocalDate.now(ZONE));
  }

  public static ClassHours compute(
      List<SessionInput> sessions,
      List<AttendanceInput> attendances,
      List<StudentInput> students,
      LocalDate asOfDate) {
    LocalDate cutoff = asOfDate == null ? LocalDate.now(ZONE) : asOfDate;
    List<SessionInput> sessionList = sessions == null ? List.of() : sessions;
    List<AttendanceInput> attendanceList = attendances == null ? List.of() : attendances;
    List<StudentInput> studentList = students == null ? List.of() : students;
    List<ResolvedSession> resolved = resolveSessions(sessionList);
    BigDecimal plannedHours = BigDecimal.ZERO;
    BigDecimal plannedHoursFromExcel = BigDecimal.ZERO;
    int unconvertible = 0;
    for (ResolvedSession session : resolved) {
      if (session.convertible()) {
        plannedHours = plannedHours.add(session.hours());
      } else {
        unconvertible++;
      }
    }
    for (SessionInput raw : sessionList) {
      BigDecimal excel = parseClassHours(raw.classHours());
      if (excel != null) {
        plannedHoursFromExcel = plannedHoursFromExcel.add(excel);
      }
    }
    plannedHours = round1(plannedHours);
    plannedHoursFromExcel = round1(plannedHoursFromExcel);

    Map<Long, List<AttendanceInput>> attByPerson = new LinkedHashMap<>();
    for (AttendanceInput row : attendanceList) {
      if (row.peopleId() == null) {
        continue;
      }
      attByPerson.computeIfAbsent(row.peopleId(), key -> new ArrayList<>()).add(row);
    }

    List<StudentHours> rows = new ArrayList<>();
    BigDecimal attendedSum = BigDecimal.ZERO;
    int recorded = 0;
    int unrecorded = 0;
    int avgN = 0;
    BigDecimal completionSum = BigDecimal.ZERO;
    int completionN = 0;
    for (StudentInput student : studentList) {
      if (student.peopleId() == null) {
        continue;
      }
      List<AttendanceInput> personAtt = attByPerson.getOrDefault(student.peopleId(), List.of());
      StudentHours row = computeStudent(student, personAtt, resolved, plannedHours, cutoff);
      rows.add(row);
      if (personAtt.isEmpty()) {
        unrecorded++;
      } else {
        recorded++;
      }
      if (row.attendedHours() != null) {
        attendedSum = attendedSum.add(row.attendedHours());
        avgN++;
      }
      if (row.completionRate() != null) {
        completionSum = completionSum.add(row.completionRate());
        completionN++;
      }
    }

    BigDecimal attendedAvg = avgN > 0 ? round1(attendedSum.divide(BigDecimal.valueOf(avgN), 4, RoundingMode.HALF_UP)) : null;
    BigDecimal hoursCompletionAvg = completionN > 0
        ? round1(completionSum.divide(BigDecimal.valueOf(completionN), 4, RoundingMode.HALF_UP))
        : null;

    return new ClassHours(
        plannedHours,
        plannedHoursFromExcel,
        unconvertible,
        recorded,
        unrecorded,
        round1(attendedSum),
        attendedAvg,
        hoursCompletionAvg,
        rows);
  }

  static StudentHours computeStudent(
      StudentInput student,
      List<AttendanceInput> personAtt,
      List<ResolvedSession> allSessions,
      BigDecimal plannedHours,
      LocalDate asOfDate) {
    Map<String, DayBundle> days = new LinkedHashMap<>();
    for (AttendanceInput row : personAtt) {
      String date = normalizeDate(row.attendanceDate());
      if (!StringUtils.hasText(date)) {
        continue;
      }
      DayBundle bundle = days.computeIfAbsent(date, key -> new DayBundle());
      if (row.scheduleId() == null) {
        bundle.dayLevel = mergeStatus(bundle.dayLevel, row);
      } else {
        bundle.sessions.merge(row.scheduleId(), row, FollowClassHourCalculator::mergeStatus);
      }
    }

    BigDecimal hours = BigDecimal.ZERO;
    int presentSessions = 0;
    int absentSessions = 0;
    boolean partial = false;
    // 仅截止日及以前：默认计入；明确缺勤扣除。未来课次不计入已上课时。
    for (ResolvedSession session : allSessions) {
      if (!isOnOrBefore(session.date(), asOfDate)) {
        continue;
      }
      if (!session.convertible()) {
        DayBundle bundle = StringUtils.hasText(session.date()) ? days.get(session.date()) : null;
        if (bundle != null
            && (bundle.dayLevel != null
                || (session.id() != null && bundle.sessions.containsKey(session.id())))) {
          partial = true;
        }
        continue;
      }
      boolean absent = isAbsentForSession(session, days);
      if (absent) {
        absentSessions++;
      } else {
        hours = hours.add(session.hours());
        presentSessions++;
      }
    }
    for (DayBundle bundle : days.values()) {
      if (bundle.dayLevel != null) {
        continue;
      }
      for (Long scheduleId : bundle.sessions.keySet()) {
        ResolvedSession session = allSessions.stream()
            .filter(s -> scheduleId.equals(s.id()))
            .findFirst()
            .orElse(null);
        if (session == null || !session.convertible()) {
          partial = true;
        }
      }
    }

    hours = round1(hours);
    BigDecimal completion = plannedHours != null && plannedHours.compareTo(BigDecimal.ZERO) > 0
        ? round1(hours.multiply(BigDecimal.valueOf(100)).divide(plannedHours, 4, RoundingMode.HALF_UP))
        : null;
    String status;
    if (personAtt.isEmpty()) {
      status = partial ? STATUS_PARTIAL : STATUS_UNRECORDED;
    } else {
      status = partial ? STATUS_PARTIAL : STATUS_OK;
    }
    return new StudentHours(
        student.peopleId(), student.name(), hours, plannedHours, completion,
        presentSessions, absentSessions, status);
  }

  /** 无日期或解析失败的课次不计入已上课时。 */
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

  /** 按日缺勤仅在无课次行时生效；有课次行则按该课次是否旷课。 */
  private static boolean isAbsentForSession(ResolvedSession session, Map<String, DayBundle> days) {
    if (!StringUtils.hasText(session.date())) {
      return false;
    }
    DayBundle bundle = days.get(session.date());
    if (bundle == null) {
      return false;
    }
    // 课次行优先：与考勤信息列表「一行一课次」口径一致
    if (!bundle.sessions.isEmpty()) {
      if (session.id() != null && bundle.sessions.containsKey(session.id())) {
        return !isPresent(bundle.sessions.get(session.id()).status());
      }
      return false;
    }
    if (bundle.dayLevel != null) {
      return !isPresent(bundle.dayLevel.status());
    }
    return false;
  }

  static List<ResolvedSession> resolveSessions(List<SessionInput> sessions) {
    List<ResolvedSession> out = new ArrayList<>();
    for (SessionInput session : sessions) {
      String date = normalizeDate(session.scheduleDate());
      out.add(new ResolvedSession(session.id(), date, sessionHours(session.startTime(), session.endTime(), session.classHours())));
    }
    return out;
  }

  static BigDecimal sessionHours(String startTime, String endTime, String classHours) {
    Integer minutes = durationMinutes(startTime, endTime);
    if (minutes != null && minutes > 0) {
      return round1(BigDecimal.valueOf(minutes).divide(FORTY_FIVE, 4, RoundingMode.HALF_UP));
    }
    return parseClassHours(classHours);
  }

  static Integer durationMinutes(String startTime, String endTime) {
    Integer start = parseMinutesFromMidnight(startTime);
    Integer end = parseMinutesFromMidnight(endTime);
    if (start == null || end == null || end <= start) {
      return null;
    }
    return end - start;
  }

  static Integer parseMinutesFromMidnight(String raw) {
    String t = normalizeTime(raw);
    if (t == null) {
      return null;
    }
    String[] parts = t.split(":");
    if (parts.length < 2) {
      return null;
    }
    try {
      int hour = Integer.parseInt(parts[0]);
      int minute = Integer.parseInt(parts[1]);
      if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
        return null;
      }
      return hour * 60 + minute;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** 对齐 {@link TrainKcbiaoScheduleParser}：830 → 08:30，8:30 → 08:30。 */
  static String normalizeTime(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    String t = raw.trim();
    if (t.matches("^\\d{3,4}$")) {
      if (t.length() == 3) {
        t = "0" + t;
      }
      return t.substring(0, 2) + ":" + t.substring(2);
    }
    if (t.matches("^\\d{1,2}:\\d{2}(:\\d{2})?$")) {
      String[] p = t.split(":");
      return String.format("%02d:%s", Integer.parseInt(p[0]), p[1]);
    }
    return t;
  }

  static String normalizeDate(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    String s = raw.trim();
    if (s.length() >= 10 && s.charAt(4) == '-' && s.charAt(7) == '-') {
      return s.substring(0, 10);
    }
    return s;
  }

  static BigDecimal parseClassHours(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    String n = raw.trim().replaceAll("[^0-9.+-]", "");
    if (!StringUtils.hasText(n) || "-".equals(n) || ".".equals(n) || "+".equals(n)) {
      return null;
    }
    try {
      return round1(new BigDecimal(n));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  static boolean isPresent(Integer status) {
    return status != null && (status == 1 || status == 2 || status == 3);
  }

  /** 与考勤导入一致：缺勤优先于出勤，避免重复行把旷课盖掉。 */
  private static AttendanceInput mergeStatus(AttendanceInput current, AttendanceInput incoming) {
    if (current == null) {
      return incoming;
    }
    if (incoming == null) {
      return current;
    }
    return worseStatus(current) >= worseStatus(incoming) ? current : incoming;
  }

  private static int worseStatus(AttendanceInput row) {
    Integer status = row == null ? null : row.status();
    if (status == null) {
      return 0;
    }
    return switch (status) {
      case 0 -> 40;
      case 2 -> 30;
      case 3 -> 20;
      case 1 -> 10;
      default -> 0;
    };
  }

  private static BigDecimal round1(BigDecimal value) {
    if (value == null) {
      return null;
    }
    return value.setScale(1, RoundingMode.HALF_UP);
  }

  private static final class DayBundle {
    private AttendanceInput dayLevel;
    private final Map<Long, AttendanceInput> sessions = new LinkedHashMap<>();
  }
}
