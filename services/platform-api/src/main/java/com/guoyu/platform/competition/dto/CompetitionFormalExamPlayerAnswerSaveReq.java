package com.guoyu.platform.competition.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record CompetitionFormalExamPlayerAnswerSaveReq(
    JsonNode answer,
    Boolean marked,
    Integer version) {
}
