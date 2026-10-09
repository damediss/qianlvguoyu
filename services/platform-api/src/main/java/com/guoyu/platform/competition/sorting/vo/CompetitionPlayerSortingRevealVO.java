package com.guoyu.platform.competition.sorting.vo;

/** 一次点击抽签的揭晓结果。排序号抽签和工位抽签共用。 */
public record CompetitionPlayerSortingRevealVO(
    CompetitionPlayerSortingTaskVO task,
    String contestantProfileId,
    String name,
    String groupName,
    String orgName,
    Integer drawOrder,
    Boolean leader,
    String workstationLabel,
    String workstation,
    String subWorkstationLabel,
    String subWorkstation,
    boolean groupCompleted,
    boolean allCompleted) {
}
