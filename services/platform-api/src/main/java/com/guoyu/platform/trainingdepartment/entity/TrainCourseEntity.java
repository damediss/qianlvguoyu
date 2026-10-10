package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 课时列表
 */
@TableName("train_course")
public class TrainCourseEntity {

  /**  主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 培训ID */
  private Long trainId;
  /** 教师身份证号 */
  private String teacherIdCard;
  /** 教师姓名 */
  private String teacherName;
  /** 教师用户ID */
  private Long teacherUserId;
  /** 课时类型 */
  private Integer type;
  /** 上课日期 */
  private String courseDate;
  /** 课时标题 */
  private String title;
  /** 开始时间 */
  private String startTime;
  /** 结束时间 */
  private String endTime;
  /** 理论学时 */
  private Integer theoryTime;
  /** 实操学时 */
  private Integer operationTime;
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
  public Long getTrainId() { return trainId; }
  public void setTrainId(Long trainId) { this.trainId = trainId; }
  public String getTeacherIdCard() { return teacherIdCard; }
  public void setTeacherIdCard(String teacherIdCard) { this.teacherIdCard = teacherIdCard; }
  public String getTeacherName() { return teacherName; }
  public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
  public Long getTeacherUserId() { return teacherUserId; }
  public void setTeacherUserId(Long teacherUserId) { this.teacherUserId = teacherUserId; }
  public Integer getType() { return type; }
  public void setType(Integer type) { this.type = type; }
  public String getCourseDate() { return courseDate; }
  public void setCourseDate(String courseDate) { this.courseDate = courseDate; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getStartTime() { return startTime; }
  public void setStartTime(String startTime) { this.startTime = startTime; }
  public String getEndTime() { return endTime; }
  public void setEndTime(String endTime) { this.endTime = endTime; }
  public Integer getTheoryTime() { return theoryTime; }
  public void setTheoryTime(Integer theoryTime) { this.theoryTime = theoryTime; }
  public Integer getOperationTime() { return operationTime; }
  public void setOperationTime(Integer operationTime) { this.operationTime = operationTime; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
