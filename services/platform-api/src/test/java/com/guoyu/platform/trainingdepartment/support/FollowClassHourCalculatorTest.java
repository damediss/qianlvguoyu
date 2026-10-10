package com.guoyu.platform.trainingdepartment.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.trainingdepartment.support.FollowClassHourCalculator.AttendanceInput;
import com.guoyu.platform.trainingdepartment.support.FollowClassHourCalculator.ClassHours;
import com.guoyu.platform.trainingdepartment.support.FollowClassHourCalculator.SessionInput;
import com.guoyu.platform.trainingdepartment.support.FollowClassHourCalculator.StudentHours;
import com.guoyu.platform.trainingdepartment.support.FollowClassHourCalculator.StudentInput;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class FollowClassHourCalculatorTest {

  private static final LocalDate AS_OF = LocalDate.of(2026, 8, 19);

  @Test
  void timeExamples() {
    assertThat(FollowClassHourCalculator.sessionHours("08:00", "08:45", null))
        .isEqualByComparingTo("1.0");
    assertThat(FollowClassHourCalculator.sessionHours("08:00", "11:15", null))
        .isEqualByComparingTo("4.3");
    assertThat(FollowClassHourCalculator.sessionHours("08:00", "12:00", null))
        .isEqualByComparingTo("5.3");
    assertThat(FollowClassHourCalculator.sessionHours("08:00", "09:30", null))
        .isEqualByComparingTo("2.0");
    assertThat(FollowClassHourCalculator.sessionHours(null, null, "4"))
        .isEqualByComparingTo("4.0");
    assertThat(FollowClassHourCalculator.sessionHours(null, null, "2学时"))
        .isEqualByComparingTo("2.0");
    assertThat(FollowClassHourCalculator.sessionHours(null, null, null)).isNull();
  }

  @Test
  void sessionPresentCountsHoursAbsentZero() {
    SessionInput session = new SessionInput(1L, "2026-08-01", "08:00", "09:30", null);
    StudentInput zhang = new StudentInput(10L, "张三");
    StudentInput li = new StudentInput(11L, "李四");
    ClassHours result = FollowClassHourCalculator.compute(
        List.of(session),
        List.of(
            new AttendanceInput(10L, "2026-08-01", 1L, 1),
            new AttendanceInput(11L, "2026-08-01", 1L, 0)),
        List.of(zhang, li),
        AS_OF);
    assertThat(hoursOf(result, 10L)).isEqualByComparingTo("2.0");
    assertThat(hoursOf(result, 11L)).isEqualByComparingTo("0.0");
    assertThat(row(result, 10L).presentSessions()).isEqualTo(1);
    assertThat(row(result, 11L).absentSessions()).isEqualTo(1);
  }

  @Test
  void lateAndEarlyCountAsFullSession() {
    SessionInput session = new SessionInput(1L, "2026-08-01", "08:00", "09:30", null);
    ClassHours result = FollowClassHourCalculator.compute(
        List.of(session),
        List.of(
            new AttendanceInput(10L, "2026-08-01", 1L, 2),
            new AttendanceInput(11L, "2026-08-01", 1L, 3)),
        List.of(new StudentInput(10L, "晚"), new StudentInput(11L, "早")),
        AS_OF);
    assertThat(hoursOf(result, 10L)).isEqualByComparingTo("2.0");
    assertThat(hoursOf(result, 11L)).isEqualByComparingTo("2.0");
  }

  @Test
  void dayLevelPresentCreditsAllSessionsThatDay() {
    List<SessionInput> sessions = List.of(
        new SessionInput(1L, "2026-08-01", "08:00", "08:45", null),
        new SessionInput(2L, "2026-08-01", "09:00", "10:30", null));
    ClassHours result = FollowClassHourCalculator.compute(
        sessions,
        List.of(new AttendanceInput(10L, "2026-08-01", null, 1)),
        List.of(new StudentInput(10L, "张三")),
        AS_OF);
    assertThat(hoursOf(result, 10L)).isEqualByComparingTo("3.0");
    assertThat(row(result, 10L).presentSessions()).isEqualTo(2);
  }

  @Test
  void sessionRowsOverrideDayLevelAbsent() {
    // 有课次行时以课次为准：按日缺勤不再吞掉课次出勤
    SessionInput session = new SessionInput(1L, "2026-08-01", "08:00", "09:30", null);
    ClassHours result = FollowClassHourCalculator.compute(
        List.of(session),
        List.of(
            new AttendanceInput(10L, "2026-08-01", null, 0),
            new AttendanceInput(10L, "2026-08-01", 1L, 1)),
        List.of(new StudentInput(10L, "张三")),
        AS_OF);
    assertThat(hoursOf(result, 10L)).isEqualByComparingTo("2.0");
    assertThat(row(result, 10L).presentSessions()).isEqualTo(1);
  }

  @Test
  void dayLevelAbsentStillAppliesWhenNoSessionRows() {
    SessionInput session = new SessionInput(1L, "2026-08-01", "08:00", "09:30", null);
    ClassHours result = FollowClassHourCalculator.compute(
        List.of(session),
        List.of(new AttendanceInput(10L, "2026-08-01", null, 0)),
        List.of(new StudentInput(10L, "张三")),
        AS_OF);
    assertThat(hoursOf(result, 10L)).isEqualByComparingTo("0.0");
  }

  @Test
  void duplicateRowsPreferAbsent() {
    SessionInput session = new SessionInput(1L, "2026-08-01", "08:00", "09:30", null);
    ClassHours result = FollowClassHourCalculator.compute(
        List.of(session),
        List.of(
            new AttendanceInput(10L, "2026-08-01", 1L, 1),
            new AttendanceInput(10L, "2026-08-01", 1L, 0)),
        List.of(new StudentInput(10L, "张三")),
        AS_OF);
    assertThat(hoursOf(result, 10L)).isEqualByComparingTo("0.0");
  }

  @Test
  void futureSessionsExcludedFromAttendedSoCompletionNotFake100() {
    List<SessionInput> sessions = List.of(
        new SessionInput(1L, "2026-08-01", "08:00", "09:30", null),
        new SessionInput(2L, "2026-08-25", "08:00", "09:30", null));
    ClassHours result = FollowClassHourCalculator.compute(
        sessions,
        List.of(),
        List.of(new StudentInput(10L, "张三")),
        AS_OF);
    assertThat(result.plannedHours()).isEqualByComparingTo("4.0");
    assertThat(hoursOf(result, 10L)).isEqualByComparingTo("2.0");
    assertThat(row(result, 10L).completionRate()).isEqualByComparingTo("50.0");
  }

  @Test
  void noAttendanceCountsPastAsNotAbsent() {
    SessionInput session = new SessionInput(1L, "2026-08-01", "08:00", "09:30", null);
    ClassHours result = FollowClassHourCalculator.compute(
        List.of(session),
        List.of(new AttendanceInput(10L, "2026-08-01", 1L, 1)),
        List.of(new StudentInput(10L, "张三"), new StudentInput(11L, "李四")),
        AS_OF);
    assertThat(row(result, 11L).hoursStatus()).isEqualTo("unrecorded");
    assertThat(hoursOf(result, 11L)).isEqualByComparingTo("2.0");
    assertThat(result.recordedStudentCount()).isEqualTo(1);
    assertThat(result.unrecordedStudentCount()).isEqualTo(1);
    assertThat(result.attendedHoursAvg()).isEqualByComparingTo("2.0");
  }

  @Test
  void unmarkedSessionStillCountsWhenSiblingAbsent() {
    List<SessionInput> sessions = List.of(
        new SessionInput(1L, "2026-08-01", "08:00", "08:45", null),
        new SessionInput(2L, "2026-08-01", "09:00", "10:30", null));
    ClassHours result = FollowClassHourCalculator.compute(
        sessions,
        List.of(new AttendanceInput(10L, "2026-08-01", 1L, 0)),
        List.of(new StudentInput(10L, "张三")),
        AS_OF);
    assertThat(hoursOf(result, 10L)).isEqualByComparingTo("2.0");
    assertThat(row(result, 10L).absentSessions()).isEqualTo(1);
    assertThat(row(result, 10L).presentSessions()).isEqualTo(1);
  }

  @Test
  void classHoursWithoutTimeNotDividedBy45() {
    SessionInput session = new SessionInput(1L, "2026-08-01", null, null, "4");
    ClassHours result = FollowClassHourCalculator.compute(
        List.of(session),
        List.of(new AttendanceInput(10L, "2026-08-01", 1L, 1)),
        List.of(new StudentInput(10L, "张三")),
        AS_OF);
    assertThat(result.plannedHours()).isEqualByComparingTo("4.0");
    assertThat(hoursOf(result, 10L)).isEqualByComparingTo("4.0");
  }

  @Test
  void unconvertibleSessionsExcludedFromPlan() {
    List<SessionInput> sessions = List.of(
        new SessionInput(1L, "2026-08-01", "08:00", "08:45", null),
        new SessionInput(2L, "2026-08-01", null, null, null));
    ClassHours result = FollowClassHourCalculator.compute(
        sessions,
        List.of(new AttendanceInput(10L, "2026-08-01", null, 1)),
        List.of(new StudentInput(10L, "张三")),
        AS_OF);
    assertThat(result.unconvertibleSessions()).isEqualTo(1);
    assertThat(result.plannedHours()).isEqualByComparingTo("1.0");
    assertThat(hoursOf(result, 10L)).isEqualByComparingTo("1.0");
    assertThat(row(result, 10L).hoursStatus()).isEqualTo("partial");
  }

  @Test
  void timeBeatsClassHoursForPlannedButExcelSumKeptSeparate() {
    SessionInput session = new SessionInput(1L, "2026-08-01", "08:00", "12:00", "4");
    ClassHours result = FollowClassHourCalculator.compute(
        List.of(session), List.of(), List.of(new StudentInput(10L, "张三")), AS_OF);
    assertThat(result.plannedHours()).isEqualByComparingTo("5.3");
    assertThat(result.plannedHoursFromExcel()).isEqualByComparingTo("4.0");
    assertThat(hoursOf(result, 10L)).isEqualByComparingTo("5.3");
  }

  private static StudentHours row(ClassHours result, long peopleId) {
    return result.students().stream()
        .filter(s -> peopleId == s.peopleId())
        .findFirst()
        .orElseThrow();
  }

  private static BigDecimal hoursOf(ClassHours result, long peopleId) {
    return row(result, peopleId).attendedHours();
  }
}
