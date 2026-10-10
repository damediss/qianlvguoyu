package com.guoyu.platform.trainingdepartment.vo;

public record TrainDivisionPlanVO(
    Long planId,
    String planNo,
    String title,
    Long targetOrgId,
    String orgName,
    Integer status,
    String statusLabel,
    Boolean enterpriseReturnReady,
    String rosterFileName,
    String shiftFileName,
    Long rosterFileId,
    Long shiftFileId,
    Long linkedTrainInfoCount,
    Long pendingRosterCount
) {
}
