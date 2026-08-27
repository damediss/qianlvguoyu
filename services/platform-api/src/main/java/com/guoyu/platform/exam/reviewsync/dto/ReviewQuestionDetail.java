package com.guoyu.platform.exam.reviewsync.dto;

import java.math.BigDecimal;

/** 题目详情（content/answer/resolve，下发前剥离答案标记） */
public record ReviewQuestionDetail(
    long id,
    Long knowledgeId,
    String type,
    Integer difficulty,
    BigDecimal score,
    String content,
    String answerJson,
    String resolve) {
}
