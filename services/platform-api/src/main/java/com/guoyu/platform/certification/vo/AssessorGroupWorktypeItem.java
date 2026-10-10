package com.guoyu.platform.certification.vo;

public record AssessorGroupWorktypeItem(
    Long scopeId,
    String workTypeName,
    Long workTypeId,
    String levelCode,
    String levelName,
    Integer sortOrder
) {
}
