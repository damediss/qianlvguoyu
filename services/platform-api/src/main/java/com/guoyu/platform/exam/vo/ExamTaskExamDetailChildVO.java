package com.guoyu.platform.exam.vo;

import java.io.Serializable;

public class ExamTaskExamDetailChildVO implements Serializable {
  private String id;          // "初审" / "终审"
  private Integer examId;
  private String examContent;
  private String examAnswer;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public Integer getExamId() { return examId; }
  public void setExamId(Integer examId) { this.examId = examId; }
  public String getExamContent() { return examContent; }
  public void setExamContent(String examContent) { this.examContent = examContent; }
  public String getExamAnswer() { return examAnswer; }
  public void setExamAnswer(String examAnswer) { this.examAnswer = examAnswer; }
}
