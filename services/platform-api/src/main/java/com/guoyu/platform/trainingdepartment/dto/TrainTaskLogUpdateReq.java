package com.guoyu.platform.trainingdepartment.dto;

/** 培训任务记录更新请求 */
public class TrainTaskLogUpdateReq {

  /** 状态 */
  private Integer status;

  /** 时间段 */
  private String times;

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
