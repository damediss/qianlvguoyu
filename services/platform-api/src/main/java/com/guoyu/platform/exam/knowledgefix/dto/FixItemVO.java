package com.guoyu.platform.exam.knowledgefix.dto;

import java.math.BigDecimal;

/** 修正清单行视图（人工定夺场景：带出原/新知识点中文名与题干摘要） */
public record FixItemVO(
    Long rowId,
    Integer detailId,
    Integer oldKnowledgeId,
    Integer courseId,
    Integer newKnowledgeId,
    String oldKnowledgeTitle,
    String newKnowledgeTitle,
    BigDecimal confidence,
    String basis,
    String llmReason,
    String status,
    String stemPreview) {
}
