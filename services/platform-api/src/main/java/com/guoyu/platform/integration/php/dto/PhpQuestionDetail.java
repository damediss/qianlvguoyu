package com.guoyu.platform.integration.php.dto;

import java.math.BigDecimal;

/** PHP 题目详情（content/answer/resolve，下发前剥离答案标记） */
public record PhpQuestionDetail(
    long id,
    Long knowledgeId,
    String type,
    Integer difficulty,
    BigDecimal score,
    String content,
    String answerJson,
    String resolve) {
}
