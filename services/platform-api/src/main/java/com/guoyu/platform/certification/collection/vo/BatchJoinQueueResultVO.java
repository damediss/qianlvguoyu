package com.guoyu.platform.certification.collection.vo;

import java.util.List;

public record BatchJoinQueueResultVO(
    int successCount,
    int failureCount,
    List<BatchJoinQueueFailureVO> failures
) {
  public record BatchJoinQueueFailureVO(
      Long candidateId,
      String candidateName,
      String admissionTicketNo,
      List<String> missingDocumentCodes,
      List<String> invalidDocumentCodes,
      String message
  ) {
  }
}
