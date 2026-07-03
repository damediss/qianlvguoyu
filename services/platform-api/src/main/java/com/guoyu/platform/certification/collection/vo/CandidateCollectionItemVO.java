package com.guoyu.platform.certification.collection.vo;

public record CandidateCollectionItemVO(
    Long candidateId,
    Long taskId,
    String name,
    String idCardLast4,
    String maskedIdCardNo,
    String unitName,
    String workType,
    String level,
    String admissionNo,
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
    Integer queueNo
) {
}
