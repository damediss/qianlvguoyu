package com.guoyu.platform.exam.reviewsync.dto;

import java.util.List;

/** 考试计划（train_exam_list）与其绑定的题目范围 */
public record ReviewExamPlanInfo(
    long id,
    String title,
    Integer source,
    String examDate,
    String examDateList,
    List<WorkTypeBinding> bindings) {

  /** 工种等级绑定（train_exam_work_type_list 行） */
  public record WorkTypeBinding(long workTypeId, Integer level, Long courseId) {
  }

  /** 考试计划头部字段（漂移检测的轻量载体，只查计划行不查工种绑定） */
  public record ExamPlanHead(String examDate, String examDateList, Integer source) {
  }
}
