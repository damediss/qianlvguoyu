package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

@TableName("competition_practical_question_score_item")
public class CompetitionPracticalQuestionScoreItemEntity extends BaseEntity {

  private Long questionId;
  private Integer itemNo;
  private String itemName;
  private String criterion;
  private BigDecimal maxScore;
  private BigDecimal maxDeduction;
  private BigDecimal deductionStep;
  private String status;

  public Long getQuestionId() { return questionId; }
  public void setQuestionId(Long questionId) { this.questionId = questionId; }
  public Integer getItemNo() { return itemNo; }
  public void setItemNo(Integer itemNo) { this.itemNo = itemNo; }
  public String getItemName() { return itemName; }
  public void setItemName(String itemName) { this.itemName = itemName; }
  public String getCriterion() { return criterion; }
  public void setCriterion(String criterion) { this.criterion = criterion; }
  public BigDecimal getMaxScore() { return maxScore; }
  public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }
  public BigDecimal getMaxDeduction() { return maxDeduction; }
  public void setMaxDeduction(BigDecimal maxDeduction) { this.maxDeduction = maxDeduction; }
  public BigDecimal getDeductionStep() { return deductionStep; }
  public void setDeductionStep(BigDecimal deductionStep) { this.deductionStep = deductionStep; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
