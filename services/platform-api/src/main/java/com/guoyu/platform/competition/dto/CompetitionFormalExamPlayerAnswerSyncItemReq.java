package com.guoyu.platform.competition.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record CompetitionFormalExamPlayerAnswerSyncItemReq(
    String questionSnapshotId,
    JsonNode answer,
    Boolean marked,
    Integer version) {
}
