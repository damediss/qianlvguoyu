package com.guoyu.platform.competition.sorting.vo;

public record CompetitionPlayerSortingAssignmentVO(
    String contestantProfileId,
    String contestantNo,
    String name,
    String mobile,
    int groupNo,
    int groupOrder,
    String workstation) {
}
