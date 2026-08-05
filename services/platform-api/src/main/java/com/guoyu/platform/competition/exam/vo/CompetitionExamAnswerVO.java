package com.guoyu.platform.competition.exam.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CompetitionExamAnswerVO(
    Long questionId,
    List<String> answerKeys,
    boolean markedForReview,
    Integer saveVersion,
    LocalDateTime savedAt) {
}
