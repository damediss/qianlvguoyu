package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("competition_subject_score_import_row")
public class CompetitionSubjectScoreImportRowEntity extends BaseEntity {
  private Long batchId;
  private Integer rowNo;
  private String idCardNo;
  private String playerName;
  private Long contestantMemberId;
  private BigDecimal rawScore;
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
  public String getIdCardNo() { return idCardNo; }
  public void setIdCardNo(String value) { idCardNo = value; }
  public String getPlayerName() { return playerName; }
  public void setPlayerName(String value) { playerName = value; }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long value) { contestantMemberId = value; }
  public BigDecimal getRawScore() { return rawScore; }
  public void setRawScore(BigDecimal value) { rawScore = value; }
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
