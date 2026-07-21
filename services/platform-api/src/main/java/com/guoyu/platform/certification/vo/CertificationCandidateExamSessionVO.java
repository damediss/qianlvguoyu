package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationCandidateExamSessionEntity;

public record CertificationCandidateExamSessionVO(
    Long id,
    Long candidateId,
    Long examSessionId,
    String examType,
    String sessionName,
    String participationStatus,
    String collectionStatus,
    Long collectionSourceSessionId,
    String exemptReason,
    String remark
) {
  public static CertificationCandidateExamSessionVO from(
      CertificationCandidateExamSessionEntity relation,
      CertificationExamSessionVO session
  ) {
    return new CertificationCandidateExamSessionVO(relation.getId(), relation.getCandidateId(),
        relation.getExamSessionId(), session.examType(), session.sessionName(), relation.getParticipationStatus(),
        relation.getCollectionStatus(), relation.getCollectionSourceSessionId(), relation.getExemptReason(),
        relation.getRemark());
  }
}
