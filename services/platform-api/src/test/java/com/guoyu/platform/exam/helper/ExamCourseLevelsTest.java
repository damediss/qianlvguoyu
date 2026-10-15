package com.guoyu.platform.exam.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** exam_course.level 序数（0=初级 … 4=高级技师）→ 国标等级（5=初级工 … 1=高级技师）换算。 */
class ExamCourseLevelsTest {

  @Test
  @DisplayName("各序数换算正确：0→5级(初级) … 4→1级(高级技师)，高级=3级")
  void convertsOrdinalToNationalGrade() {
    assertEquals(5, ExamCourseLevels.toNationalGrade(0));
    assertEquals(4, ExamCourseLevels.toNationalGrade(1));
    assertEquals(3, ExamCourseLevels.toNationalGrade(2));
    assertEquals(2, ExamCourseLevels.toNationalGrade(3));
    assertEquals(1, ExamCourseLevels.toNationalGrade(4));
  }

  @Test
  @DisplayName("null 与越界值返回 null（调用方按不展示处理）")
  void outOfRangeReturnsNull() {
    assertNull(ExamCourseLevels.toNationalGrade(null));
    assertNull(ExamCourseLevels.toNationalGrade(-1));
    assertNull(ExamCourseLevels.toNationalGrade(5));
  }
}
