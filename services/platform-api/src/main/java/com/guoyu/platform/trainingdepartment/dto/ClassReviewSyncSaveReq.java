package com.guoyu.platform.trainingdepartment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** 保存班级同步学配置（绑定 exam + 启用开关 + 可选开练窗口） */
public class ClassReviewSyncSaveReq {

  @NotNull(message = "请选择同步学考试计划")
  private Long examId;

  /** 1 启用 / 0 停用；默认启用 */
  private Integer enabled = 1;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate syncStartTime;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate syncEndTime;

  private String remark;

  public Long getExamId() { return examId; }
  public void setExamId(Long examId) { this.examId = examId; }
  public Integer getEnabled() { return enabled; }
  public void setEnabled(Integer enabled) { this.enabled = enabled; }
  public LocalDate getSyncStartTime() { return syncStartTime; }
  public void setSyncStartTime(LocalDate syncStartTime) { this.syncStartTime = syncStartTime; }
  public LocalDate getSyncEndTime() { return syncEndTime; }
  public void setSyncEndTime(LocalDate syncEndTime) { this.syncEndTime = syncEndTime; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}
