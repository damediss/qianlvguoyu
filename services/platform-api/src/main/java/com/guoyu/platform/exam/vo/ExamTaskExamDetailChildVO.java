package com.guoyu.platform.exam.vo;

import java.io.Serializable;

public class ExamTaskExamDetailChildVO implements Serializable {
  private String id;          // "初审" / "终审"
  private Integer examId;
  private Integer expertId;
  private String expertName;
  private String addtimeCn;
  private String examContent;
  private String examAnswer;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public Integer getExamId() { return examId; }
  public void setExamId(Integer examId) { this.examId = examId; }
  public Integer getExpertId() { return expertId; }
  public void setExpertId(Integer expertId) { this.expertId = expertId; }
  public String getExpertName() { return expertName; }
  public void setExpertName(String expertName) { this.expertName = expertName; }
  public String getAddtimeCn() { return addtimeCn; }
  public void setAddtimeCn(String addtimeCn) { this.addtimeCn = addtimeCn; }
  public String getExamContent() { return examContent; }
  public void setExamContent(String examContent) { this.examContent = examContent; }
  public String getExamAnswer() { return examAnswer; }
  public void setExamAnswer(String examAnswer) { this.examAnswer = examAnswer; }
}
