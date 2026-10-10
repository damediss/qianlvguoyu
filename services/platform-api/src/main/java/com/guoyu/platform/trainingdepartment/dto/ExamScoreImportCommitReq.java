package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class ExamScoreImportCommitReq {

  @NotBlank
  private String previewId;
  private List<Long> peopleIds;

  public String getPreviewId() {
    return previewId;
  }

  public void setPreviewId(String previewId) {
    this.previewId = previewId;
  }

  public List<Long> getPeopleIds() {
    return peopleIds;
  }

  public void setPeopleIds(List<Long> peopleIds) {
    this.peopleIds = peopleIds;
  }
}
