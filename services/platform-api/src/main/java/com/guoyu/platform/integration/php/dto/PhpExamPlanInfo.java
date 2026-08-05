package com.guoyu.platform.integration.php.dto;

import java.util.List;

/** PHP 考试计划（train_exam_list）与其绑定的题目范围 */
public record PhpExamPlanInfo(
    long id,
    String title,
    Integer source,
    String examDate,
    String examDateList,
    List<WorkTypeBinding> bindings) {

  /** 工种等级绑定（train_exam_work_type_list 行） */
  public record WorkTypeBinding(long workTypeId, Integer level, Long courseId) {
  }
}
