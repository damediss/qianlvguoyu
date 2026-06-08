package com.guoyu.platform.certification.dto;

import java.util.List;

public record PreparationAssessorGroupSaveReq(
    String groupName,
    Long leaderAssessorId,
    List<Long> assessorIds,
    String workTypeName,
    String status,
    String remark
) {
}
