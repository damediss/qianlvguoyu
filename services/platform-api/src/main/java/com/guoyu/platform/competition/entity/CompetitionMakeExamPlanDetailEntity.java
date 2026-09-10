package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

@TableName("competition_make_exam_plan_detail")
public class CompetitionMakeExamPlanDetailEntity extends BaseEntity {

  private Long planId;
  private String questionType;
  private Integer questionCount;
  private BigDecimal point;
  private Integer difficultyStart;
  private Integer difficultyEnd;
  private Integer sortOrder;

  public Long getPlanId() { return planId; }
  public void setPlanId(Long planId) { this.planId = planId; }
  public String getQuestionType() { return questionType; }
  public void setQuestionType(String questionType) { this.questionType = questionType; }
  public Integer getQuestionCount() { return questionCount; }
  public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
  public BigDecimal getPoint() { return point; }
  public void setPoint(BigDecimal point) { this.point = point; }
  public Integer getDifficultyStart() { return difficultyStart; }
  public void setDifficultyStart(Integer difficultyStart) { this.difficultyStart = difficultyStart; }
  public Integer getDifficultyEnd() { return difficultyEnd; }
  public void setDifficultyEnd(Integer difficultyEnd) { this.difficultyEnd = difficultyEnd; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
