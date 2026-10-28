package com.guoyu.platform.competition.prepractice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_pre_practice_task_question")
public class CompetitionPrePracticeTaskQuestionEntity extends BaseEntity {

  private Long taskId;
  private Long sourceQuestionId;
  private Integer sourceCourseId;
  private Integer sourceWorkTypeId;
  private Integer sourceLevel;
  private String questionType;
  private Integer sortOrder;
  private String questionSnapshotJson;

  public Long getTaskId() { return taskId; }
  public void setTaskId(Long taskId) { this.taskId = taskId; }
  public Long getSourceQuestionId() { return sourceQuestionId; }
  public void setSourceQuestionId(Long sourceQuestionId) { this.sourceQuestionId = sourceQuestionId; }
  public Integer getSourceCourseId() { return sourceCourseId; }
  public void setSourceCourseId(Integer sourceCourseId) { this.sourceCourseId = sourceCourseId; }
  public Integer getSourceWorkTypeId() { return sourceWorkTypeId; }
  public void setSourceWorkTypeId(Integer sourceWorkTypeId) { this.sourceWorkTypeId = sourceWorkTypeId; }
  public Integer getSourceLevel() { return sourceLevel; }
  public void setSourceLevel(Integer sourceLevel) { this.sourceLevel = sourceLevel; }
  public String getQuestionType() { return questionType; }
  public void setQuestionType(String questionType) { this.questionType = questionType; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
  public String getQuestionSnapshotJson() { return questionSnapshotJson; }
  public void setQuestionSnapshotJson(String questionSnapshotJson) {
    this.questionSnapshotJson = questionSnapshotJson;
  }
}
