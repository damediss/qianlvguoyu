package com.guoyu.platform.competition.sorting.vo;

/** 一次点击抽签的揭晓结果。排序号抽签和工位抽签共用。 */
public record CompetitionPlayerSortingRevealVO(
    CompetitionPlayerSortingTaskVO task,
    String contestantProfileId,
    String name,
    String groupName,
    String orgName,
    Integer drawOrder,
    /** 历史字段。排序号抽签不再把 1 号标为组长，新结果为 null。 */
    Boolean leader,
    String workstationLabel,
    String workstation,
    String subWorkstationLabel,
    String subWorkstation,
    boolean groupCompleted,
    boolean allCompleted) {
}
