package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** 培训任务记录创建请求 */
public class TrainTaskLogCreateReq {

  /** 培训班级ID */
  private Long trainClassId;

  /** 签到ID */
  private Long ckId;

  @Size(max = 10)
  private String date;

  /** 添加时间 */
  private Integer addtime;

  /** 状态 */
  private Integer status;

  /** 时间段 */
  private String times;

  public Long getTrainClassId() {
    return trainClassId;
  }

  public void setTrainClassId(Long trainClassId) {
    this.trainClassId = trainClassId;
  }

  public Long getCkId() {
    return ckId;
  }

  public void setCkId(Long ckId) {
    this.ckId = ckId;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public Integer getAddtime() {
    return addtime;
  }

  public void setAddtime(Integer addtime) {
    this.addtime = addtime;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public String getTimes() {
    return times;
  }

  public void setTimes(String times) {
    this.times = times;
  }

}
