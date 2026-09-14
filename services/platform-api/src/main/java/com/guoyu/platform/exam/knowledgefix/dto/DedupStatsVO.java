package com.guoyu.platform.exam.knowledgefix.dto;

/** 去重统计卡 */
public record DedupStatsVO(long mayTotalRows, long uniqGroups, long deleteRows, long manualReviewRows) {
}
