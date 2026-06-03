package com.guoyu.platform.exam.vo;

import java.io.Serializable;

public class ExamTaskExamsubDetailChildVO implements Serializable {
  private String id;
  private Integer examId;
  private String content;
  private String answer;
  private String pic;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public Integer getExamId() { return examId; }
  public void setExamId(Integer examId) { this.examId = examId; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getAnswer() { return answer; }
  public void setAnswer(String answer) { this.answer = answer; }
  public String getPic() { return pic; }
  public void setPic(String pic) { this.pic = pic; }
}
