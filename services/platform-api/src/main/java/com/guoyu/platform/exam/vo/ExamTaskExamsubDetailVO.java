package com.guoyu.platform.exam.vo;

import java.io.Serializable;
import java.util.List;

public class ExamTaskExamsubDetailVO implements Serializable {

  private Integer id;
  private Integer taskId;
  private String zhishidian;
  private String content;
  private String answer;
  private Integer isJisuan;
  private String nanyicd;
  private Integer isblod;
  private String pic;
  private List<ExamTaskExamsubDetailChildVO> children;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getTaskId() { return taskId; }
  public void setTaskId(Integer taskId) { this.taskId = taskId; }
  public String getZhishidian() { return zhishidian; }
  public void setZhishidian(String zhishidian) { this.zhishidian = zhishidian; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getAnswer() { return answer; }
  public void setAnswer(String answer) { this.answer = answer; }
  public Integer getIsJisuan() { return isJisuan; }
  public void setIsJisuan(Integer isJisuan) { this.isJisuan = isJisuan; }
  public String getNanyicd() { return nanyicd; }
  public void setNanyicd(String nanyicd) { this.nanyicd = nanyicd; }
  public Integer getIsblod() { return isblod; }
  public void setIsblod(Integer isblod) { this.isblod = isblod; }
  public String getPic() { return pic; }
  public void setPic(String pic) { this.pic = pic; }
  public List<ExamTaskExamsubDetailChildVO> getChildren() { return children; }
  public void setChildren(List<ExamTaskExamsubDetailChildVO> children) { this.children = children; }
}
