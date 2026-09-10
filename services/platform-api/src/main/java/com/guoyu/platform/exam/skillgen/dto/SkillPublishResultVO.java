package com.guoyu.platform.exam.skillgen.dto;

/** 入库结果 */
public record SkillPublishResultVO(
    int published,
    int skipped,
    Long taskId,
    /** 入库后该（工种,等级）累计有效明细数（全部来源，S10 口径） */
    long cumulativeTotal,
    /** 其中 AI 生成（source=2）数 */
    long cumulativeAiTotal) {
}
