package com.guoyu.platform.certification.collection.vo;

public record CandidateMatchVO(
    Long candidateId,
    Long taskId,
    String name,
    String idCardLast4,
    String maskedIdCardNo,
    String unitName,
    String workType,
    String level,
    String admissionNo,
    Long avatarFileId,
    String currentStatus,
    String currentStatusLabel,
    Boolean alreadyQueued,
    Integer queueNo,
    String qrIssueStatus,
    String qrIssueStatusLabel,
    String matchMethod,
    String matchMethodLabel,
    String documentStatus,
    java.util.List<String> missingDocumentCodes,
    java.util.List<String> invalidDocumentCodes,
    Boolean collectionEligible
) {
}
