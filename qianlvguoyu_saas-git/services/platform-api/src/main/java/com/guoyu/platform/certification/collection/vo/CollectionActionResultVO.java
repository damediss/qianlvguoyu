package com.guoyu.platform.certification.collection.vo;

public record CollectionActionResultVO(
    Long taskId,
    Long candidateId,
    String overallStatus,
    String overallStatusLabel,
    String queueStatus,
    String queueStatusLabel,
    String fingerprintStatus,
    String fingerprintStatusLabel,
    String signatureStatus,
    String signatureStatusLabel,
    String exceptionStatus,
    String exceptionStatusLabel,
    QueueItemVO queueItem,
    String message
) {
}
