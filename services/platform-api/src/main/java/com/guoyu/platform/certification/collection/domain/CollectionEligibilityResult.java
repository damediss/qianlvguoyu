package com.guoyu.platform.certification.collection.domain;

import java.util.List;
import java.util.Map;

/** Unified result for the practical collection admission gate. */
public record CollectionEligibilityResult(
    boolean eligible,
    Long candidateId,
    String candidateName,
    String admissionTicketNo,
    String documentStatus,
    List<String> missingDocumentCodes,
    List<String> invalidDocumentCodes,
    String reasonCode,
    String reasonMessage,
    Map<String, Long> validDocumentFileIds
) {
  public Long avatarFileId() {
    return validDocumentFileIds == null ? null : validDocumentFileIds.get("DOC002");
  }
}
