package com.guoyu.platform.exam.knowledgefix.dto;

import java.math.BigDecimal;

/** 修正清单行视图 */
public record FixItemVO(
    Long rowId,
    Integer detailId,
    Integer oldKnowledgeId,
    Integer courseId,
    Integer newKnowledgeId,
    BigDecimal confidence,
    String basis,
    String llmReason,
    String status,
    String stemPreview) {
}
