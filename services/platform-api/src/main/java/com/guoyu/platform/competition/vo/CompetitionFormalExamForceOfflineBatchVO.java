package com.guoyu.platform.competition.vo;

/** 批量清退正式比赛选手考试账号结果。 */
public record CompetitionFormalExamForceOfflineBatchVO(
    int requestedCount,
    int kickedCount,
    int alreadyOfflineCount) {
}
