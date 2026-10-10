package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_event_work_type_stage")
public class CompetitionEventWorkTypeStageEntity extends BaseEntity {
  private Long eventId;
  private Long eventWorkTypeId;
  private String eventWorkTypeLevelCode;
  private Long stageId;
  private Integer enabled;
  private Integer sortOrder;
  private String promotionRuleType;
  private Integer promotionPercentage;
  private Integer legacyRule;
  private Integer promotionQuota;

  public Long getEventId() { return eventId; }
  public void setEventId(Long value) { eventId = value; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long value) { eventWorkTypeId = value; }
  public String getEventWorkTypeLevelCode() { return eventWorkTypeLevelCode; }
  public void setEventWorkTypeLevelCode(String value) { eventWorkTypeLevelCode = value; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long value) { stageId = value; }
  public Integer getEnabled() { return enabled; }
  public void setEnabled(Integer value) { enabled = value; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer value) { sortOrder = value; }
  public String getPromotionRuleType() { return promotionRuleType; }
  public void setPromotionRuleType(String value) { promotionRuleType = value; }
  public Integer getPromotionPercentage() { return promotionPercentage; }
  public void setPromotionPercentage(Integer value) { promotionPercentage = value; }
  public Integer getLegacyRule() { return legacyRule; }
  public void setLegacyRule(Integer value) { legacyRule = value; }
  public Integer getPromotionQuota() { return promotionQuota; }
  public void setPromotionQuota(Integer value) { promotionQuota = value; }
}
