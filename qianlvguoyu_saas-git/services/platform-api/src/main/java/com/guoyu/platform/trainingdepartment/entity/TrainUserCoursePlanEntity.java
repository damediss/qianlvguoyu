package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户课程计划
 */
@TableName("train_user_course_plan")
public class TrainUserCoursePlanEntity {

  /**  主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 用户ID */
  private Long userId;
  /** 平台用户ID */
  private Long platformUserId;
  /** 真实姓名 */
  private String realname;
  /** 添加来源 */
  private Integer addFrom;
  /** 课程ID */
  private Long courseId;
  /** 日期 */
  private String date;
  /** 类型 */
  private Integer type;
  /** 状态 */
  private Integer status;
  /** 排序号 */
  private Integer sortNumber;
  /** 租户ID */
  private Long tenantId;
  /** 创建时间 */
  private LocalDateTime createTime;

  /**  逻辑删除标记 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getPlatformUserId() { return platformUserId; }
  public void setPlatformUserId(Long platformUserId) { this.platformUserId = platformUserId; }
  public String getRealname() { return realname; }
  public void setRealname(String realname) { this.realname = realname; }
  public Integer getAddFrom() { return addFrom; }
  public void setAddFrom(Integer addFrom) { this.addFrom = addFrom; }
  public Long getCourseId() { return courseId; }
  public void setCourseId(Long courseId) { this.courseId = courseId; }
  public String getDate() { return date; }
  public void setDate(String date) { this.date = date; }
  public Integer getType() { return type; }
  public void setType(Integer type) { this.type = type; }
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
