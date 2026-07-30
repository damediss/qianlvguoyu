package com.guoyu.platform.exam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpertAuditSubmitReq {

  private Integer id;
  private Integer taskId;
  private Integer role;

  // exam fields
  private String examType;
  private String examContent;
  private String examA;
  private String examB;
  private String examC;
  private String examD;
  private String examE;
  private String examF;
  private String examG;
  private String examH;
  private String examAnswer;

  // examshicao fields
  private String jianding;
  private String content;
  private String pingfen;
  private String kaochang;
  private String kaosheng;

  // examsub fields
  private String answer;
  private String pic;

  // ximu fields
  private String sno;
  private String snoItem;
  private String snoTitle;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getTaskId() { return taskId; }
  public void setTaskId(Integer taskId) { this.taskId = taskId; }
  public Integer getRole() { return role; }
  public void setRole(Integer role) { this.role = role; }
  public String getExamType() { return examType; }
  public void setExamType(String examType) { this.examType = examType; }
  public String getExamContent() { return examContent; }
  public void setExamContent(String examContent) { this.examContent = examContent; }
  public String getExamA() { return examA; }
  public void setExamA(String examA) { this.examA = examA; }
  public String getExamB() { return examB; }
  public void setExamB(String examB) { this.examB = examB; }
  public String getExamC() { return examC; }
  public void setExamC(String examC) { this.examC = examC; }
  public String getExamD() { return examD; }
  public void setExamD(String examD) { this.examD = examD; }
  public String getExamE() { return examE; }
  public void setExamE(String examE) { this.examE = examE; }
  public String getExamF() { return examF; }
  public void setExamF(String examF) { this.examF = examF; }
  public String getExamG() { return examG; }
  public void setExamG(String examG) { this.examG = examG; }
  public String getExamH() { return examH; }
  public void setExamH(String examH) { this.examH = examH; }
  public String getExamAnswer() { return examAnswer; }
  public void setExamAnswer(String examAnswer) { this.examAnswer = examAnswer; }
  public String getJianding() { return jianding; }
  public void setJianding(String jianding) { this.jianding = jianding; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getPingfen() { return pingfen; }
  public void setPingfen(String pingfen) { this.pingfen = pingfen; }
  public String getKaochang() { return kaochang; }
  public void setKaochang(String kaochang) { this.kaochang = kaochang; }
  public String getKaosheng() { return kaosheng; }
  public void setKaosheng(String kaosheng) { this.kaosheng = kaosheng; }
  public String getAnswer() { return answer; }
  public void setAnswer(String answer) { this.answer = answer; }
  public String getPic() { return pic; }
  public void setPic(String pic) { this.pic = pic; }
  public String getSno() { return sno; }
  public void setSno(String sno) { this.sno = sno; }
  public String getSnoItem() { return snoItem; }
  public void setSnoItem(String snoItem) { this.snoItem = snoItem; }
  public String getSnoTitle() { return snoTitle; }
  public void setSnoTitle(String snoTitle) { this.snoTitle = snoTitle; }
}
