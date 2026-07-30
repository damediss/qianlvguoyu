package com.guoyu.platform.exam.vo;

import com.guoyu.platform.exam.dto.CourseItem;
import java.time.LocalDateTime;
import java.util.List;

public class ExamUserAuthCourseVO {

  private String realname;
  private String idCard;
  private List<CourseItem> courses;
  private Integer status;
  private Integer sortNumber;
  private LocalDateTime createTime;

  public String getRealname() { return realname; }
  public void setRealname(String realname) { this.realname = realname; }
  public String getIdCard() { return idCard; }
  public void setIdCard(String idCard) { this.idCard = idCard; }
  public List<CourseItem> getCourses() { return courses; }
  public void setCourses(List<CourseItem> courses) { this.courses = courses; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}