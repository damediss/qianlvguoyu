package com.guoyu.platform.exam.theorygen.dto;

/** 入库结果 */
public record TheoryPublishResultVO(
    int published,
    int objectiveCount,
    int subjectiveCount,
    int skipped,
    Long examTaskId,
    Long examsubTaskId) {
}
