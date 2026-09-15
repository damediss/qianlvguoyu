package com.guoyu.platform.exam.knowledgefix.dto;

/**
 * 去重统计卡（FIX-5 口径修正，2026-09-15）：
 * 旧版把「组数」标成"唯一题数"造成误读——本 VO 明确区分：
 * dupGroups = 重复题组数（同课程口径，一组=同一课程内同一道题的 N 份副本）；
 * deleteRows = 当前清单待执行删除行数（pending/approved）；deletedRows = 已执行软删行数（done，可回滚）。
 */
public record DedupStatsVO(
    long mayTotalRows,
    long dupGroups,
    long deleteRows,
    long alreadyDeletedRows,
    long manualReviewRows) {
}
