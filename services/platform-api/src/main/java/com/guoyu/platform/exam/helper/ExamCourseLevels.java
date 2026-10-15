package com.guoyu.platform.exam.helper;

/**
 * exam_course.level（序数 0=初级 … 4=高级技师）与国家职业技能等级（5=初级工 … 1=高级技师）的换算。
 * 全库口径以序数存储（见 ReviewQuestionQueryService / TheoryBlueprintApplicationService），
 * 仅对外展示与命题导出模板（表头"级别（5：初级…1：高级技师）"）使用国标数字。
 */
public final class ExamCourseLevels {

  private ExamCourseLevels() {
  }

  /** 序数 → 国标等级（0→5 级 … 4→1 级）；null 或越界返回 null，调用方按"不展示"处理 */
  public static Integer toNationalGrade(Integer courseLevel) {
    if (courseLevel == null || courseLevel < 0 || courseLevel > 4) {
      return null;
    }
    return 5 - courseLevel;
  }
}
