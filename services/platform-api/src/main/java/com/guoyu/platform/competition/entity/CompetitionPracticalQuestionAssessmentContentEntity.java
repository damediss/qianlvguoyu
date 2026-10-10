package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_practical_question_assessment_content")
public class CompetitionPracticalQuestionAssessmentContentEntity extends BaseEntity {

  private Long questionId;
  private Integer sortOrder;
  private String contentHtml;

  public Long getQuestionId() { return questionId; }
  public void setQuestionId(Long questionId) { this.questionId = questionId; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
  public String getContentHtml() { return contentHtml; }
  public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }
}
