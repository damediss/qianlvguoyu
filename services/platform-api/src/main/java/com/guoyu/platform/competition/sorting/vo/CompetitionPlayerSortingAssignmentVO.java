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
    /** 手动选择的组内角色。点击抽签流程不再使用。 */
    String role,
    /** 子工位编号，与主工位同属一个组。 */
    String subWorkstation) {
}
