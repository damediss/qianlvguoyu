package com.guoyu.platform.certification.collection.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CandidateCollectionDetailVO(
    Long statusId,
    Long candidateId,
    Long taskId,
    Long sessionId,
    Long examSessionId,
    String name,
    String idCardLast4,
    String maskedIdCardNo,
    String unitName,
    String workType,
    String level,
    String admissionNo,
    Long avatarFileId,
    Boolean hasCollectToken,
    String qrIssueStatus,
    String qrIssueStatusLabel,
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
    Integer queueNo,
    LocalDateTime arrivedAt,
    LocalDateTime collectingStartedAt,
    LocalDateTime completedAt,
    QueueItemVO currentQueue,
    FingerprintTraceVO latestFingerprint,
    SignatureRecordVO latestSignature,
    List<CollectionExceptionVO> exceptions,
    String documentStatus,
    List<String> missingDocumentCodes,
    List<String> invalidDocumentCodes,
    Boolean collectionEligible
) {
}
