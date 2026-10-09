package com.guoyu.platform.competition.sorting.vo;

public record CompetitionPlayerSortingAssignmentVO(
    String contestantProfileId,
    String contestantNo,
    String name,
    String mobile,
    int groupNo,
    int groupOrder,
    String workstation,
    /** 组内随机指定的组长。历史快照没有该字段时为 null。 */
    Boolean groupLeader,
    String orgName,
    String groupName,
    /** 手动选择的组内角色。与组长标记相互独立。 */
    String role) {
}
