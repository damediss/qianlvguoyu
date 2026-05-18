package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 培训任务记录
 */
@TableName("train_task_log")
public class TrainTaskLogEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 班级ID */
  private Long trainClassId;
  /** 签到ID */
  private Long ckId;
  /** 日期 */
  private String date;
  /** 添加时间 */
  private Integer addtime;
  /** 状态 */
  private Integer status;
  /** 时间段 */
  private String times;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
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
