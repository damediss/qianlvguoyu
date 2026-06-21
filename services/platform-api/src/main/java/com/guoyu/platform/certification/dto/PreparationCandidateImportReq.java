package com.guoyu.platform.certification.dto;

import java.util.List;

public record PreparationCandidateImportReq(
    String sourceType,
    Long sourceFileId,
    List<PreparationCandidateSaveReq> candidates,
    List<Long> trainingPeopleIds,
    Long taskScopeId,
    String remark
) {
  public PreparationCandidateImportReq(
      String sourceType, Long sourceFileId, List<PreparationCandidateSaveReq> candidates,
      List<Long> trainingPeopleIds, String remark
  ) {
    this(sourceType, sourceFileId, candidates, trainingPeopleIds, null, remark);
  }
}
