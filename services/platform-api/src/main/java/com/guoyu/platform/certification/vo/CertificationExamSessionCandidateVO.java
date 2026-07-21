package com.guoyu.platform.certification.vo;

public record CertificationExamSessionCandidateVO(
    Long candidateId,
    String candidateName,
    String idCardNo,
    String admissionTicketNo,
    String participationStatus,
    String collectionStatus
) {
}
