package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("train_class_practice_course")
public class TrainClassPracticeCourseEntity {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long tenantId;
  private Long classId;
  private Integer examCourseId;
  private LocalDateTime createTime;
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Long getClassId() { return classId; }
  public void setClassId(Long classId) { this.classId = classId; }
  public Integer getExamCourseId() { return examCourseId; }
  public void setExamCourseId(Integer examCourseId) { this.examCourseId = examCourseId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
