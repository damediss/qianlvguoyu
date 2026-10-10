package com.guoyu.platform.competition.exam.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

public record CompetitionExamQuestionVO(
    Long id,
    Integer sortOrder,
    String questionType,
    String content,
    JsonNode options,
    BigDecimal score) {
}
