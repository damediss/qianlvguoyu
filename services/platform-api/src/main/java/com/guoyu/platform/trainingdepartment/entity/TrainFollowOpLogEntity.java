package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 跟班过程操作日志（系统自动写入） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_follow_op_log")
public class TrainFollowOpLogEntity extends BaseEntity {

  private Long classId;
  private Long scheduleId;
  private Long sessionId;
  private String bizDate;
  private String eventCode;
  private String eventCategory;
  private String title;
  private String summary;
  private String detailJson;
  private Long actorUserId;
  private String actorName;
  private String actorRole;
  private String targetType;
  private Long targetId;
  /** info / warn / critical */
  private String severityLevel;
  /** admin / app / job */
  private String client;
  private String requestId;
}
