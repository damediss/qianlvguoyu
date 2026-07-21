package com.guoyu.platform.certification.dto;

import java.util.List;

public record PreparationCandidateImportReq(
    String sourceType,
    Long sourceFileId,
    List<PreparationCandidateSaveReq> candidates,
    List<Long> trainingPeopleIds,
    Long taskScopeId,
    List<Long> examSessionIds,
    String defaultParticipation,
    String remark
) {
  public PreparationCandidateImportReq(
      String sourceType, Long sourceFileId, List<PreparationCandidateSaveReq> candidates,
      List<Long> trainingPeopleIds, String remark
  ) {
    this(sourceType, sourceFileId, candidates, trainingPeopleIds, null, null, null, remark);
  }

  public PreparationCandidateImportReq(
      String sourceType, Long sourceFileId, List<PreparationCandidateSaveReq> candidates,
      List<Long> trainingPeopleIds, Long taskScopeId, String remark
  ) {
    this(sourceType, sourceFileId, candidates, trainingPeopleIds, taskScopeId, null, null, remark);
  }
}
