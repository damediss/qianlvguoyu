package com.guoyu.platform.exam.vo;

import java.io.Serializable;

public class ExamTaskExamshicaoDetailChildVO implements Serializable {
  private String id;
  private Integer examId;
  private String content;
  private String pingfen;
  private String kaochang;
  private String kaosheng;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public Integer getExamId() { return examId; }
  public void setExamId(Integer examId) { this.examId = examId; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getPingfen() { return pingfen; }
  public void setPingfen(String pingfen) { this.pingfen = pingfen; }
  public String getKaochang() { return kaochang; }
  public void setKaochang(String kaochang) { this.kaochang = kaochang; }
  public String getKaosheng() { return kaosheng; }
  public void setKaosheng(String kaosheng) { this.kaosheng = kaosheng; }
}
