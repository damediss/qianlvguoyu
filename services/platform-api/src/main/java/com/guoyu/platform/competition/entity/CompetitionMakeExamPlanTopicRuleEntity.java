package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

/** 组卷计划按细目表主题的题量与分值规则。 */
@TableName("competition_make_exam_plan_topic_rule")
public class CompetitionMakeExamPlanTopicRuleEntity extends BaseEntity {

  private Long planId;
  private Long blueprintNodeId;
  private Integer questionCount;
  private BigDecimal score;
  private Integer selected;
  private Integer sortOrder;
  public Long getPlanId() { return planId; }
  public void setPlanId(Long planId) { this.planId = planId; }
  public Long getBlueprintNodeId() { return blueprintNodeId; }
  public void setBlueprintNodeId(Long blueprintNodeId) { this.blueprintNodeId = blueprintNodeId; }
  public Integer getQuestionCount() { return questionCount; }
  public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
  public BigDecimal getScore() { return score; }
  public void setScore(BigDecimal score) { this.score = score; }
  public Integer getSelected() { return selected; }
  public void setSelected(Integer selected) { this.selected = selected; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
