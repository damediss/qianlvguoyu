package com.guoyu.platform.competition.draw.vo;

public record CompetitionPlayerDrawSlot(
    String contestantProfileId,
    String contestantNo,
    String name,
    String orgName,
    String mobile,
    int drawOrder,
    Integer appearanceOrder) {
}
