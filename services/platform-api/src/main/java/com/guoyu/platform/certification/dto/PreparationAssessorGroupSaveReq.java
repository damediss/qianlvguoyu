package com.guoyu.platform.certification.dto;

import java.util.List;

public record PreparationAssessorGroupSaveReq(
    String groupName,
    Long leaderAssessorId,
    List<Long> assessorIds,
    @Deprecated String workTypeName,
    List<Long> workTypeScopeIds,
    String status,
    String remark
) {
}
