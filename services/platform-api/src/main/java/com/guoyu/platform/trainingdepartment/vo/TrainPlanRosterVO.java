package com.guoyu.platform.trainingdepartment.vo;

public record TrainPlanRosterVO(
    Long id,
    Long planId,
    Long trainInfoId,
    String source,
    String personName,
    String idCard,
    String phone,
    String jobName,
    String levelName,
    String workshop,
    String shiftTeam,
    Integer planYear,
    String trainMode,
    Integer assignStatus,
    Long trainClassId
) {
}
