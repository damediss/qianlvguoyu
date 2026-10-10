package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

@TableName("competition_practical_execution_config")
public class CompetitionPracticalExecutionConfigEntity extends BaseEntity {
  private Long eventId;
  private Long stageId;
  private Long stageContentId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Long scoringSchemeId;
  private String examName;
  private Long durationSeconds;
  private String startMode;
  private Integer allowEarlySubmit;
  private Integer timeScoringEnabled;
  private String overtimeRuleType;
  private Long overtimeStepSeconds;
  private BigDecimal overtimeScorePerStep;
  private BigDecimal overtimeFixedDeduction;
  private BigDecimal overtimeMaxDeduction;
  private String earlyFinishRuleType;
  private Long earlyFinishStepSeconds;
  private BigDecimal earlyFinishScorePerStep;
  private BigDecimal earlyFinishFixedBonus;
  private BigDecimal earlyFinishMaxBonus;
  private String overtimeAction;
  private Long maxDurationSeconds;
  private BigDecimal finalScoreUpperLimit;
  private Integer version;

  public Long getEventId() { return eventId; }
  public void setEventId(Long value) { eventId = value; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long value) { stageId = value; }
  public Long getStageContentId() { return stageContentId; }
  public void setStageContentId(Long value) { stageContentId = value; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long value) { eventWorkTypeId = value; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long value) { eventWorkTypeLevelId = value; }
  public Long getScoringSchemeId() { return scoringSchemeId; }
  public void setScoringSchemeId(Long value) { scoringSchemeId = value; }
  public String getExamName() { return examName; }
  public void setExamName(String value) { examName = value; }
  public Long getDurationSeconds() { return durationSeconds; }
  public void setDurationSeconds(Long value) { durationSeconds = value; }
  public String getStartMode() { return startMode; }
  public void setStartMode(String value) { startMode = value; }
  public Integer getAllowEarlySubmit() { return allowEarlySubmit; }
  public void setAllowEarlySubmit(Integer value) { allowEarlySubmit = value; }
  public Integer getTimeScoringEnabled() { return timeScoringEnabled; }
  public void setTimeScoringEnabled(Integer value) { timeScoringEnabled = value; }
  public String getOvertimeRuleType() { return overtimeRuleType; }
  public void setOvertimeRuleType(String value) { overtimeRuleType = value; }
  public Long getOvertimeStepSeconds() { return overtimeStepSeconds; }
  public void setOvertimeStepSeconds(Long value) { overtimeStepSeconds = value; }
  public BigDecimal getOvertimeScorePerStep() { return overtimeScorePerStep; }
  public void setOvertimeScorePerStep(BigDecimal value) { overtimeScorePerStep = value; }
  public BigDecimal getOvertimeFixedDeduction() { return overtimeFixedDeduction; }
  public void setOvertimeFixedDeduction(BigDecimal value) { overtimeFixedDeduction = value; }
  public BigDecimal getOvertimeMaxDeduction() { return overtimeMaxDeduction; }
  public void setOvertimeMaxDeduction(BigDecimal value) { overtimeMaxDeduction = value; }
  public String getEarlyFinishRuleType() { return earlyFinishRuleType; }
  public void setEarlyFinishRuleType(String value) { earlyFinishRuleType = value; }
  public Long getEarlyFinishStepSeconds() { return earlyFinishStepSeconds; }
  public void setEarlyFinishStepSeconds(Long value) { earlyFinishStepSeconds = value; }
  public BigDecimal getEarlyFinishScorePerStep() { return earlyFinishScorePerStep; }
  public void setEarlyFinishScorePerStep(BigDecimal value) { earlyFinishScorePerStep = value; }
  public BigDecimal getEarlyFinishFixedBonus() { return earlyFinishFixedBonus; }
  public void setEarlyFinishFixedBonus(BigDecimal value) { earlyFinishFixedBonus = value; }
  public BigDecimal getEarlyFinishMaxBonus() { return earlyFinishMaxBonus; }
  public void setEarlyFinishMaxBonus(BigDecimal value) { earlyFinishMaxBonus = value; }
  public String getOvertimeAction() { return overtimeAction; }
  public void setOvertimeAction(String value) { overtimeAction = value; }
  public Long getMaxDurationSeconds() { return maxDurationSeconds; }
  public void setMaxDurationSeconds(Long value) { maxDurationSeconds = value; }
  public BigDecimal getFinalScoreUpperLimit() { return finalScoreUpperLimit; }
  public void setFinalScoreUpperLimit(BigDecimal value) { finalScoreUpperLimit = value; }
  public Integer getVersion() { return version; }
  public void setVersion(Integer value) { version = value; }
}
