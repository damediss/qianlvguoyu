package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record CompetitionFormalExamPlayerSavedAnswerVO(
    String questionSnapshotId,
    JsonNode answer,
    boolean marked,
    int version,
    boolean isAnswered,
    LocalDateTime savedAt) {
}
