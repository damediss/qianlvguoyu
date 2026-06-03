package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 专家端 - 提交主观题审核。 */
public class ExpertTaskExamsubSubmitReq {

  @NotNull
  private Integer taskId;
  @NotNull
  private Integer role;
  private Integer id;
  private String content;
  private String answer;
  private String pic;
  private List<SubItem> items;

  public Integer getTaskId() { return taskId; }
  public void setTaskId(Integer taskId) { this.taskId = taskId; }
  public Integer getRole() { return role; }
  public void setRole(Integer role) { this.role = role; }
  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getAnswer() { return answer; }
  public void setAnswer(String answer) { this.answer = answer; }
  public String getPic() { return pic; }
  public void setPic(String pic) { this.pic = pic; }
  public List<SubItem> getItems() { return items; }
  public void setItems(List<SubItem> items) { this.items = items; }

  public static class SubItem {
    private Integer id;
    private String content;
    private String answer;
    private String pic;
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getPic() { return pic; }
    public void setPic(String pic) { this.pic = pic; }
  }
}
