package com.guoyu.platform.app.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 用户答题明细（错题本/考点预测/学情分析基础） */
@TableName("exam_user_answer_log")
public class AppExamUserAnswerLogEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;
  private Long tenantId;
  private Long classId;
  private Long peopleId;
  private Integer courseId;
  private java.time.LocalDate bizDate;
  private Integer questionId;
  private Integer knowledgeId;
  private String questionType;
  private String userAnswer;
  private Integer isRight;
  private String source;
  private Integer paperId;
  private Long spendMs;
  private LocalDateTime createTime;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Long getClassId() { return classId; }
  public void setClassId(Long classId) { this.classId = classId; }
  public Long getPeopleId() { return peopleId; }
  public void setPeopleId(Long peopleId) { this.peopleId = peopleId; }
  public Integer getCourseId() { return courseId; }
  public void setCourseId(Integer courseId) { this.courseId = courseId; }
  public java.time.LocalDate getBizDate() { return bizDate; }
  public void setBizDate(java.time.LocalDate bizDate) { this.bizDate = bizDate; }
  public Integer getQuestionId() { return questionId; }
  public void setQuestionId(Integer questionId) { this.questionId = questionId; }
  public Integer getKnowledgeId() { return knowledgeId; }
  public void setKnowledgeId(Integer knowledgeId) { this.knowledgeId = knowledgeId; }
  public String getQuestionType() { return questionType; }
  public void setQuestionType(String questionType) { this.questionType = questionType; }
  public String getUserAnswer() { return userAnswer; }
  public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }
  public Integer getIsRight() { return isRight; }
  public void setIsRight(Integer isRight) { this.isRight = isRight; }
  public String getSource() { return source; }
  public void setSource(String source) { this.source = source; }
  public Integer getPaperId() { return paperId; }
  public void setPaperId(Integer paperId) { this.paperId = paperId; }
  public Long getSpendMs() { return spendMs; }
  public void setSpendMs(Long spendMs) { this.spendMs = spendMs; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
