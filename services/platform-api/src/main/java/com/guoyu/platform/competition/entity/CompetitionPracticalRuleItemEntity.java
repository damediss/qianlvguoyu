package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

@TableName("competition_practical_rule_item")
public class CompetitionPracticalRuleItemEntity extends BaseEntity {
  private Long ruleId;
  private String itemCode;
  private String itemName;
  private Integer sortOrder;
  private BigDecimal maxScore;
  private Integer deductionAllowed;
  private String criteriaPayload;
  private String status;
  public Long getRuleId() { return ruleId; }
  public void setRuleId(Long value) { ruleId = value; }
  public String getItemCode() { return itemCode; }
  public void setItemCode(String value) { itemCode = value; }
  public String getItemName() { return itemName; }
  public void setItemName(String value) { itemName = value; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer value) { sortOrder = value; }
  public BigDecimal getMaxScore() { return maxScore; }
  public void setMaxScore(BigDecimal value) { maxScore = value; }
  public Integer getDeductionAllowed() { return deductionAllowed; }
  public void setDeductionAllowed(Integer value) { deductionAllowed = value; }
  public String getCriteriaPayload() { return criteriaPayload; }
  public void setCriteriaPayload(String value) { criteriaPayload = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
}
