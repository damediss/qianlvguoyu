package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record CompetitionFormalExamPlayerAnswerSaveVO(
    String questionSnapshotId,
    int version,
    boolean isAnswered,
    boolean marked,
    LocalDateTime savedAt,
    int answeredCount,
    LocalDateTime serverTime,
    boolean accepted,
    String conflictCode,
    JsonNode answer,
    String status) {
}
