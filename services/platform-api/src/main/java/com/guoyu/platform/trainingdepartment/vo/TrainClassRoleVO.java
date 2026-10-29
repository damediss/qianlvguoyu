package com.guoyu.platform.trainingdepartment.vo;

/** 班级角色 VO */
public record TrainClassRoleVO(
    String id,
    Long classId,
    String roleCode,
    String roleName,
    String subjectType,
    Long subjectId,
    Long platformUserId,
    Long peopleId,
    String displayName,
    String phone,
    String status,
    boolean unboundWarning,
    /** bound | ensured | reused；列表多为 bound 或 null */
    String bindStatus
) {
}
