package com.guoyu.platform.integration.php.dto;

import java.math.BigDecimal;

/** PHP 题库题目（exam_exam_detail 轻量行，排题用） */
public record PhpQuestion(
    long id,
    Long knowledgeId,
    String type,
    Integer difficulty,
    BigDecimal score) {
}
