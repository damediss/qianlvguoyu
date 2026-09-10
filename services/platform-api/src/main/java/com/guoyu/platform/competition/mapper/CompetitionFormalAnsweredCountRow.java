package com.guoyu.platform.competition.mapper;

public class CompetitionFormalAnsweredCountRow {

  private Long candidateExamId;
  private Long total;

  public Long getCandidateExamId() {
    return candidateExamId;
  }

  public void setCandidateExamId(Long candidateExamId) {
    this.candidateExamId = candidateExamId;
  }

  public Long getTotal() {
    return total;
  }

  public void setTotal(Long total) {
    this.total = total;
  }

  public int totalOrZero() {
    return total == null ? 0 : total.intValue();
  }
}
