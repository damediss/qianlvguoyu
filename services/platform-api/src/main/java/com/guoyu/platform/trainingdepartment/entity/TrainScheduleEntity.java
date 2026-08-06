package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 培训排课实体
 */
@TableName("train_schedule")
public class TrainScheduleEntity {

    /**  主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 班级ID */
    private Long classId;
    /** 课程ID */
    private Long courseId;
    /** 讲师ID */
    private Long teacherId;
    /** 讲师姓名 */
    private String teacherName;
  /** 课程名称 */
  private String courseName;
  /** 学时（开班课表 Excel 原值，如 2 / 2.0） */
  private String classHours;
  /** 授课形式（理论/实操等） */
  private String teachForm;
  private String roomName;
  private Integer dayOfWeek;
  /** 开始时间 */
  private String startTime;
  /** 结束时间 */
  private String endTime;
  private String scheduleDate;
  /** 状态 */
  private Integer status;
  /**  租户ID */
  @TableField(fill = FieldFill.INSERT)
  private Long tenantId;
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;
  @TableField(fill = FieldFill.INSERT)
  private Long createdBy;
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private Long updatedBy;
  /**  逻辑删除标记 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getClassId() { return classId; }
  public void setClassId(Long classId) { this.classId = classId; }
  public Long getCourseId() { return courseId; }
  public void setCourseId(Long courseId) { this.courseId = courseId; }
  public Long getTeacherId() { return teacherId; }
  public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
  public String getTeacherName() { return teacherName; }
  public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
  public String getCourseName() { return courseName; }
  public void setCourseName(String courseName) { this.courseName = courseName; }
  public String getClassHours() { return classHours; }
  public void setClassHours(String classHours) { this.classHours = classHours; }
  public String getTeachForm() { return teachForm; }
  public void setTeachForm(String teachForm) { this.teachForm = teachForm; }
  public String getRoomName() { return roomName; }
  public void setRoomName(String roomName) { this.roomName = roomName; }
    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getScheduleDate() { return scheduleDate; }
    public void setScheduleDate(String scheduleDate) { this.scheduleDate = scheduleDate; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
