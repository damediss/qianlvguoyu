package com.guoyu.platform.exam.reviewsync.dto;

import java.math.BigDecimal;

/** 题库题目（exam_exam_detail 轻量行，排题用） */
public record ReviewQuestion(
    long id,
    Long knowledgeId,
    String type,
    Integer difficulty,
    BigDecimal score) {
}
