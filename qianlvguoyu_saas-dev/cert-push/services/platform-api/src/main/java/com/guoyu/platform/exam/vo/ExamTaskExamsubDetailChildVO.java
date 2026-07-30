package com.guoyu.platform.exam.vo;

import java.io.Serializable;

public class ExamTaskExamsubDetailChildVO implements Serializable {
  private String id;
  private Integer examId;
  private Integer expertId;
  private String expertName;
  private String addtimeCn;
  private String content;
  private String answer;
  private String pic;

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
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getAnswer() { return answer; }
  public void setAnswer(String answer) { this.answer = answer; }
  public String getPic() { return pic; }
  public void setPic(String pic) { this.pic = pic; }
}
