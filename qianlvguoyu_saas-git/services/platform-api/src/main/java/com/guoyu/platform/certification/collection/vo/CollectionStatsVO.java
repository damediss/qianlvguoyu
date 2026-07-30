package com.guoyu.platform.certification.collection.vo;

public record CollectionStatsVO(
    Integer candidateTotal,
    Integer qrIssuedCount,
    Integer arrivedCount,
    Integer queuedCount,
    Integer collectingCount,
    Integer completedCount,
    Integer pendingRetryCount,
    Integer infoErrorCount,
    Integer exceptionReleasedCount,
    Integer exceptionCount
) {
}
