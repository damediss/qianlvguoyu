package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 专家端 - 提交实操题审核。 */
public class ExpertTaskExamshicaoSubmitReq {

  @NotNull
  private Integer taskId;
  @NotNull
  private Integer role;
  private Integer id;
  private String jianding;
  private String content;
  private String pingfen;
  private String kaochang;
  private String kaosheng;
  private List<ShicaoItem> items;

  public Integer getTaskId() { return taskId; }
  public void setTaskId(Integer taskId) { this.taskId = taskId; }
  public Integer getRole() { return role; }
  public void setRole(Integer role) { this.role = role; }
  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
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
  public List<ShicaoItem> getItems() { return items; }
  public void setItems(List<ShicaoItem> items) { this.items = items; }

  public static class ShicaoItem {
    private Integer id;
    private String jianding;
    private String content;
    private String pingfen;
    private String kaochang;
    private String kaosheng;
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
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
  }
}
