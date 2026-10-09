package com.guoyu.platform.competition.sorting.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CompetitionPlayerSortingTaskVO(
    String id,
    String eventId,
    String stageId,
    String stageName,
    Integer groupCount,
    Integer workstationCount,
    Integer playersPerGroup,
    Integer playerCount,
    String status,
    LocalDateTime createdAt,
    List<CompetitionPlayerSortingAssignmentVO> assignments,
    /** company：按参赛公司分组；balanced：历史的均衡分组。 */
    String groupingMode,
    String workstationLabel,
    List<String> workstationCodes,
    List<String> roleOptions,
    List<CompetitionPlayerSortingMemberVO> members,
    String subWorkstationLabel,
    List<String> subWorkstationCodes,
    List<CompetitionPlayerSortingWorkstationVO> stations) {
}
