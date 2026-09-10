package com.guoyu.platform.exam.theorygen.dto;

import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;

/** 生成任务可选模型配置（下拉，仅启用项） */
public record TheoryModelConfigVO(
    Long id,
    String modelCode,
    String modelName,
    String modelId,
    String thinking,
    String reasoningEffort) {

  public static TheoryModelConfigVO from(ExamFaqModelConfigEntity e) {
    return new TheoryModelConfigVO(e.getId(), e.getModelCode(), e.getModelName(),
        e.getModelId(), e.getThinking(), e.getReasoningEffort());
  }
}
