package com.guoyu.platform.competition.classify.vo;

public record CompetitionPlayerClassifyAssignmentVO(
    String contestantProfileId,
    String contestantNo,
    String name,
    String orgName,
    int typeIndex,
    String typeName,
    int orderInType) {
}
