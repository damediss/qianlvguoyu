package com.guoyu.platform.certification.dto;

public record PreparationAssessorGroupSaveReq(
    String groupName,
    Long leaderAssessorId,
    String workTypeName,
    String status,
    String remark
) {
}
