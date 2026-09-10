package com.guoyu.platform.competition.dto;

import java.util.List;

public record CompetitionFormalExamPlayerAnswerSyncReq(
    List<CompetitionFormalExamPlayerAnswerSyncItemReq> answers) {
}
