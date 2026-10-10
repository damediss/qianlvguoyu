package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("competition_simulation_score_import_row")
public class CompetitionSimulationScoreImportRowEntity extends BaseEntity {
  private Long batchId;
  private Integer rowNo;
  private String candidateNo;
  private Long contestantMemberId;
  private BigDecimal simulationScore;
  private String externalReference;
  private LocalDateTime achievedAt;
  private String remark;
  private String validationStatus;
  private String errorMessage;
  private Long scoreSourceId;

  public Long getBatchId() { return batchId; }
  public void setBatchId(Long value) { batchId = value; }
  public Integer getRowNo() { return rowNo; }
  public void setRowNo(Integer value) { rowNo = value; }
  public String getCandidateNo() { return candidateNo; }
  public void setCandidateNo(String value) { candidateNo = value; }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long value) { contestantMemberId = value; }
  public BigDecimal getSimulationScore() { return simulationScore; }
  public void setSimulationScore(BigDecimal value) { simulationScore = value; }
  public String getExternalReference() { return externalReference; }
  public void setExternalReference(String value) { externalReference = value; }
  public LocalDateTime getAchievedAt() { return achievedAt; }
  public void setAchievedAt(LocalDateTime value) { achievedAt = value; }
  public String getRemark() { return remark; }
  public void setRemark(String value) { remark = value; }
  public String getValidationStatus() { return validationStatus; }
  public void setValidationStatus(String value) { validationStatus = value; }
  public String getErrorMessage() { return errorMessage; }
  public void setErrorMessage(String value) { errorMessage = value; }
  public Long getScoreSourceId() { return scoreSourceId; }
  public void setScoreSourceId(Long value) { scoreSourceId = value; }
}
