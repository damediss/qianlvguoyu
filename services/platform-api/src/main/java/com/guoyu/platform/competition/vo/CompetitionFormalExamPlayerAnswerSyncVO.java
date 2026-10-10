package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CompetitionFormalExamPlayerAnswerSyncVO(
    List<CompetitionFormalExamPlayerAnswerSaveVO> results,
    LocalDateTime serverTime) {
}
