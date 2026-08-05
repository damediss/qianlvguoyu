package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

@TableName("competition_theory_requirement")
public class CompetitionTheoryRequirementEntity extends BaseEntity {

  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Long scoreRuleId;
  private String templateCode;
  private Integer durationMinutes;
  private Integer singleCount;
  private BigDecimal singleScore;
  private Integer multiCount;
  private BigDecimal multiScore;
  private Integer judgeCount;
  private BigDecimal judgeScore;
  private BigDecimal totalScore;
  private BigDecimal passScore;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) { this.eventWorkTypeLevelId = eventWorkTypeLevelId; }
  public Long getScoreRuleId() { return scoreRuleId; }
  public void setScoreRuleId(Long scoreRuleId) { this.scoreRuleId = scoreRuleId; }
  public String getTemplateCode() { return templateCode; }
  public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
  public Integer getDurationMinutes() { return durationMinutes; }
  public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
  public Integer getSingleCount() { return singleCount; }
  public void setSingleCount(Integer singleCount) { this.singleCount = singleCount; }
  public BigDecimal getSingleScore() { return singleScore; }
  public void setSingleScore(BigDecimal singleScore) { this.singleScore = singleScore; }
  public Integer getMultiCount() { return multiCount; }
  public void setMultiCount(Integer multiCount) { this.multiCount = multiCount; }
  public BigDecimal getMultiScore() { return multiScore; }
  public void setMultiScore(BigDecimal multiScore) { this.multiScore = multiScore; }
  public Integer getJudgeCount() { return judgeCount; }
  public void setJudgeCount(Integer judgeCount) { this.judgeCount = judgeCount; }
  public BigDecimal getJudgeScore() { return judgeScore; }
  public void setJudgeScore(BigDecimal judgeScore) { this.judgeScore = judgeScore; }
  public BigDecimal getTotalScore() { return totalScore; }
  public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
  public BigDecimal getPassScore() { return passScore; }
  public void setPassScore(BigDecimal passScore) { this.passScore = passScore; }
}
