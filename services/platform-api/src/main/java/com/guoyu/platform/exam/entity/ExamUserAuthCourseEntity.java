package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("exam_user_auth_course")
public class ExamUserAuthCourseEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer uid;
  private String realname;
  private Integer courseId;
  private String courseTitle;
  private Integer type;
  private Integer userId;
  private String idCard;
  private Integer status;
  private Integer sortNumber;
  private Long tenantId;
  private LocalDateTime createTime;

  @TableLogic(value = "0", delval = "1")
  private Integer deleted;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getUid() { return uid; }
  public void setUid(Integer uid) { this.uid = uid; }
  public String getRealname() { return realname; }
  public void setRealname(String realname) { this.realname = realname; }
  public Integer getCourseId() { return courseId; }
  public void setCourseId(Integer courseId) { this.courseId = courseId; }
  public String getCourseTitle() { return courseTitle; }
  public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
  public Integer getType() { return type; }
  public void setType(Integer type) { this.type = type; }
  public Integer getUserId() { return userId; }
  public void setUserId(Integer userId) { this.userId = userId; }
  public String getIdCard() { return idCard; }
  public void setIdCard(String idCard) { this.idCard = idCard; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}