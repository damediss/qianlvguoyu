package com.guoyu.platform.exam.vo;

import java.io.Serializable;
import java.util.List;

public class ExamTaskExamshicaoDetailVO implements Serializable {

  private Integer id;
  private Integer taskId;
  private String xingwei;
  private String kaohe1;
  private String kaohe2;
  private String kaohe3;
  private String kaohe4;
  private String jianding;
  private String nanyicd;
  private String content;
  private String pingfen;
  private String kaochang;
  private String kaosheng;
  private Integer isblod;
  private List<ExamTaskExamshicaoDetailChildVO> children;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getTaskId() { return taskId; }
  public void setTaskId(Integer taskId) { this.taskId = taskId; }
  public String getXingwei() { return xingwei; }
  public void setXingwei(String xingwei) { this.xingwei = xingwei; }
  public String getKaohe1() { return kaohe1; }
  public void setKaohe1(String kaohe1) { this.kaohe1 = kaohe1; }
  public String getKaohe2() { return kaohe2; }
  public void setKaohe2(String kaohe2) { this.kaohe2 = kaohe2; }
  public String getKaohe3() { return kaohe3; }
  public void setKaohe3(String kaohe3) { this.kaohe3 = kaohe3; }
  public String getKaohe4() { return kaohe4; }
  public void setKaohe4(String kaohe4) { this.kaohe4 = kaohe4; }
  public String getJianding() { return jianding; }
  public void setJianding(String jianding) { this.jianding = jianding; }
  public String getNanyicd() { return nanyicd; }
  public void setNanyicd(String nanyicd) { this.nanyicd = nanyicd; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getPingfen() { return pingfen; }
  public void setPingfen(String pingfen) { this.pingfen = pingfen; }
  public String getKaochang() { return kaochang; }
  public void setKaochang(String kaochang) { this.kaochang = kaochang; }
  public String getKaosheng() { return kaosheng; }
  public void setKaosheng(String kaosheng) { this.kaosheng = kaosheng; }
  public Integer getIsblod() { return isblod; }
  public void setIsblod(Integer isblod) { this.isblod = isblod; }
  public List<ExamTaskExamshicaoDetailChildVO> getChildren() { return children; }
  public void setChildren(List<ExamTaskExamshicaoDetailChildVO> children) { this.children = children; }
}
