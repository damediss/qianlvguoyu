package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_practical_question_detail")
public class CompetitionPracticalQuestionDetailEntity extends BaseEntity {

  private Long questionId;
  private String operationContent;
  private String studentPrepare;
  private String equipmentPrepare;

  public Long getQuestionId() { return questionId; }
  public void setQuestionId(Long questionId) { this.questionId = questionId; }
  public String getOperationContent() { return operationContent; }
  public void setOperationContent(String operationContent) { this.operationContent = operationContent; }
  public String getStudentPrepare() { return studentPrepare; }
  public void setStudentPrepare(String studentPrepare) { this.studentPrepare = studentPrepare; }
  public String getEquipmentPrepare() { return equipmentPrepare; }
  public void setEquipmentPrepare(String equipmentPrepare) { this.equipmentPrepare = equipmentPrepare; }
}
