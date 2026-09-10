package com.guoyu.platform.exam.skillgen.dto;

import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;

/** 生成任务可选模型配置（下拉，仅启用项） */
public record SkillModelConfigVO(
    Long id,
    String modelCode,
    String modelName,
    String modelId,
    String thinking,
    String reasoningEffort) {

  public static SkillModelConfigVO from(ExamFaqModelConfigEntity e) {
    return new SkillModelConfigVO(e.getId(), e.getModelCode(), e.getModelName(),
        e.getModelId(), e.getThinking(), e.getReasoningEffort());
  }
}
