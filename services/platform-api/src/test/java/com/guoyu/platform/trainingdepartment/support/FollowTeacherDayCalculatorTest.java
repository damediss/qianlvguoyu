package com.guoyu.platform.trainingdepartment.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.trainingdepartment.support.FollowTeacherDayCalculator.ClassTeacherDays;
import com.guoyu.platform.trainingdepartment.support.FollowTeacherDayCalculator.SessionInput;
import com.guoyu.platform.trainingdepartment.support.FollowTeacherDayCalculator.TeacherFormDays;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class FollowTeacherDayCalculatorTest {

  private static final LocalDate AS_OF = LocalDate.of(2026, 8, 19);

  @Test
  void fourPastSessionsTwoDays() {
    ClassTeacherDays result = FollowTeacherDayCalculator.compute(
        List.of(
            FollowTeacherDayCalculator.session("2026-08-10", "张三"),
            FollowTeacherDayCalculator.session("2026-08-10", "张三"),
            FollowTeacherDayCalculator.session("2026-08-11", "张三"),
            FollowTeacherDayCalculator.session("2026-08-11", "张三")),
        AS_OF);
    TeacherFormDays zhang = first(result, "张三");
    assertThat(zhang.taughtSessionCount()).isEqualTo(4);
    assertThat(zhang.taughtDays()).isEqualByComparingTo("2.0");
  }

  @Test
  void sameDayThreeSessionsCappedAtTwoWithOverflow() {
    ClassTeacherDays result = FollowTeacherDayCalculator.compute(
        List.of(
            new SessionInput("2026-08-10", "08:00", "张三", "理论"),
            new SessionInput("2026-08-10", "13:00", "张三", "实操"),
            new SessionInput("2026-08-10", "19:30", "张三", "理论")),
        AS_OF);
    assertThat(result.overflowSessionCount()).isEqualTo(1);
    TeacherFormDays theory = row(result, "张三", "理论");
    TeacherFormDays practice = row(result, "张三", "实操");
    assertThat(theory.taughtDays()).isEqualByComparingTo("0.5");
    assertThat(practice.taughtDays()).isEqualByComparingTo("0.5");
    assertThat(result.taughtDaysSum()).isEqualByComparingTo("1.0");
  }

  @Test
  void futureSessionsOnlyInPlanned() {
    ClassTeacherDays result = FollowTeacherDayCalculator.compute(
        List.of(
            FollowTeacherDayCalculator.session("2026-08-10", "李四"),
            FollowTeacherDayCalculator.session("2026-08-25", "李四"),
            FollowTeacherDayCalculator.session("2026-08-25", "李四")),
        AS_OF);
    TeacherFormDays li = first(result, "李四");
    assertThat(li.taughtSessionCount()).isEqualTo(1);
    assertThat(li.taughtDays()).isEqualByComparingTo("0.5");
    assertThat(li.plannedSessionCount()).isEqualTo(3);
    assertThat(li.plannedDays()).isEqualByComparingTo("1.5");
  }

  @Test
  void blankTeacherGoesUnassigned() {
    ClassTeacherDays result = FollowTeacherDayCalculator.compute(
        List.of(
            FollowTeacherDayCalculator.session("2026-08-10", "张三"),
            FollowTeacherDayCalculator.session("2026-08-10", "  "),
            FollowTeacherDayCalculator.session("2026-08-10", null)),
        AS_OF);
    assertThat(result.assignedSessionCount()).isEqualTo(1);
    assertThat(result.unassignedSessionCount()).isEqualTo(2);
  }

  @Test
  void nameTrimMergesFullWidthSpace() {
    ClassTeacherDays result = FollowTeacherDayCalculator.compute(
        List.of(
            FollowTeacherDayCalculator.session("2026-08-10", "张三"),
            FollowTeacherDayCalculator.session("2026-08-11", "　张三　")),
        AS_OF);
    assertThat(result.teachers()).hasSize(1);
    assertThat(first(result, "张三").plannedSessionCount()).isEqualTo(2);
  }

  @Test
  void normalizeTeachForm() {
    assertThat(FollowTeacherDayCalculator.normalizeTeachForm(null)).isEqualTo("未填");
    assertThat(FollowTeacherDayCalculator.normalizeTeachForm("理论授课")).isEqualTo("理论");
    assertThat(FollowTeacherDayCalculator.normalizeTeachForm("实操")).isEqualTo("实操");
  }

  private static TeacherFormDays first(ClassTeacherDays result, String name) {
    return result.teachers().stream()
        .filter(t -> name.equals(t.teacherName()))
        .findFirst()
        .orElseThrow();
  }

  private static TeacherFormDays row(ClassTeacherDays result, String name, String form) {
    return result.teachers().stream()
        .filter(t -> name.equals(t.teacherName()) && form.equals(t.teachForm()))
        .findFirst()
        .orElseThrow();
  }
}
