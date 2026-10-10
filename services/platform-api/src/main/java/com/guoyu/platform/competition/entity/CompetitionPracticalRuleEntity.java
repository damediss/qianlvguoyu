package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("competition_practical_rule")
public class CompetitionPracticalRuleEntity extends BaseEntity {
  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Integer ruleVersion;
  private String ruleName;
  private BigDecimal totalScore;
  private String status;
  private LocalDateTime publishedAt;
  private Long publishedBy;
  public Long getEventId() { return eventId; }
  public void setEventId(Long value) { eventId = value; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long value) { stageId = value; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long value) { eventWorkTypeId = value; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long value) { eventWorkTypeLevelId = value; }
  public Integer getRuleVersion() { return ruleVersion; }
  public void setRuleVersion(Integer value) { ruleVersion = value; }
  public String getRuleName() { return ruleName; }
  public void setRuleName(String value) { ruleName = value; }
  public BigDecimal getTotalScore() { return totalScore; }
  public void setTotalScore(BigDecimal value) { totalScore = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
  public LocalDateTime getPublishedAt() { return publishedAt; }
  public void setPublishedAt(LocalDateTime value) { publishedAt = value; }
  public Long getPublishedBy() { return publishedBy; }
  public void setPublishedBy(Long value) { publishedBy = value; }
}
