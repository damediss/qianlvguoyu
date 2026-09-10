package com.guoyu.platform.competition.vo;

public record CompetitionFormalExamAnswerVersionMismatchVO(
    String questionSnapshotId,
    Integer clientVersion,
    Integer serverVersion) {
}
