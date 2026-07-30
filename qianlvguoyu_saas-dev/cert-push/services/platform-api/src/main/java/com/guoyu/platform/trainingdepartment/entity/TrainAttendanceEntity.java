package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 培训考勤实体
 */
@TableName("train_attendance")
public class TrainAttendanceEntity {

  /**  主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 项目ID */
  private Long projectId;
  /** 班级ID */
  private Long classId;
  /** 用户ID */
  private Long userId;
  private String userName;
  private String attendanceDate;
  /** 状态 */
  private Integer status;
  private String checkInTime;
  private String checkOutTime;
  private String photoPath;
  private String videoPath;
  /** 备注 */
  private String remark;
  /** 排序号 */
  private Integer sortNumber;
  /** 租户ID */
  private Long tenantId;
  private Long createdBy;
  /** 创建时间 */
  private LocalDateTime createTime;
  /** 更新时间 */
  private LocalDateTime updateTime;

  /**  逻辑删除标记 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public Long getClassId() { return classId; }
  public void setClassId(Long classId) { this.classId = classId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getUserName() { return userName; }
  public void setUserName(String userName) { this.userName = userName; }
  public String getAttendanceDate() { return attendanceDate; }
  public void setAttendanceDate(String attendanceDate) { this.attendanceDate = attendanceDate; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public String getCheckInTime() { return checkInTime; }
  public void setCheckInTime(String checkInTime) { this.checkInTime = checkInTime; }
  public String getCheckOutTime() { return checkOutTime; }
  public void setCheckOutTime(String checkOutTime) { this.checkOutTime = checkOutTime; }
  public String getPhotoPath() { return photoPath; }
  public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
  public String getVideoPath() { return videoPath; }
  public void setVideoPath(String videoPath) { this.videoPath = videoPath; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
