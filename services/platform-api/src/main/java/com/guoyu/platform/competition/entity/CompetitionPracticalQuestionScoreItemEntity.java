package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

@TableName("competition_practical_question_score_item")
public class CompetitionPracticalQuestionScoreItemEntity extends BaseEntity {

  private Long questionId;
  private Long contentId;
  private Integer elementOrder;
  private Integer itemNo;
  private String itemName;
  private String elementHtml;
  private Integer allocationEnabled;
  private String criterion;
  private String standardHtml;
  private BigDecimal maxScore;
  private BigDecimal maxDeduction;
  private BigDecimal deductionStep;
  private String status;

  public Long getQuestionId() { return questionId; }
  public void setQuestionId(Long questionId) { this.questionId = questionId; }
  public Long getContentId() { return contentId; }
  public void setContentId(Long contentId) { this.contentId = contentId; }
  public Integer getElementOrder() { return elementOrder; }
  public void setElementOrder(Integer elementOrder) { this.elementOrder = elementOrder; }
  public Integer getItemNo() { return itemNo; }
  public void setItemNo(Integer itemNo) { this.itemNo = itemNo; }
  public String getItemName() { return itemName; }
  public void setItemName(String itemName) { this.itemName = itemName; }
  public String getElementHtml() { return elementHtml; }
  public void setElementHtml(String elementHtml) { this.elementHtml = elementHtml; }
  public Integer getAllocationEnabled() { return allocationEnabled; }
  public void setAllocationEnabled(Integer allocationEnabled) { this.allocationEnabled = allocationEnabled; }
  public String getCriterion() { return criterion; }
  public void setCriterion(String criterion) { this.criterion = criterion; }
  public String getStandardHtml() { return standardHtml; }
  public void setStandardHtml(String standardHtml) { this.standardHtml = standardHtml; }
  public BigDecimal getMaxScore() { return maxScore; }
  public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }
  public BigDecimal getMaxDeduction() { return maxDeduction; }
  public void setMaxDeduction(BigDecimal maxDeduction) { this.maxDeduction = maxDeduction; }
  public BigDecimal getDeductionStep() { return deductionStep; }
  public void setDeductionStep(BigDecimal deductionStep) { this.deductionStep = deductionStep; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
