package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** train_task_log 更新请求 */
public class TrainTaskLogUpdateReq {

  @Size(max = 255)
  /** 班级ID */
  private Long trainClassId;

  @Size(max = 255)
  /** 任务检查点ID */
  private Long ckId;

  @Size(max = 255)
  /** 日期 */
  private String date;

  @Size(max = 255)
  /** 添加时间戳 */
  private Integer addtime;

  @Size(max = 255)
  /** 状态 */
  private Integer status;

  @Size(max = 255)
  /** 时间范围 */
  private String times;
  public Long getTrainClassId() { return trainClassId; }
  public void setTrainClassId(Long trainClassId) { this.trainClassId = trainClassId; }
  public Long getCkId() { return ckId; }
  public void setCkId(Long ckId) { this.ckId = ckId; }
  public String getDate() { return date; }
  public void setDate(String date) { this.date = date; }
  public Integer getAddtime() { return addtime; }
  public void setAddtime(Integer addtime) { this.addtime = addtime; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public String getTimes() { return times; }
  public void setTimes(String times) { this.times = times; }
}
