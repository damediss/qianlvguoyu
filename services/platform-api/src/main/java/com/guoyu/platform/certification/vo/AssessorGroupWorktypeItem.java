package com.guoyu.platform.certification.vo;

public record AssessorGroupWorktypeItem(
    Long scopeId,
    String workTypeName,
    String levelCode,
    String levelName,
    Integer sortOrder
) {
}
