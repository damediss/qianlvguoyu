package com.guoyu.platform.trainingdepartment.vo;

/**
 * 培训排课视图对象
 */
public record TrainScheduleConflictVO(
    Long scheduleId1,
    Long scheduleId2,
    String conflictType,
    /** 描述 */
    String description
) {}
