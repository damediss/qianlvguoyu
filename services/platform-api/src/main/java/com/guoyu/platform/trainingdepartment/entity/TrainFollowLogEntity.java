package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 跟班/班主任工作日志（人工） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_follow_log")
public class TrainFollowLogEntity extends BaseEntity {

  private Long classId;
  private Long scheduleId;
  private Long sessionId;
  private String bizDate;
  private Long authorUserId;
  private String authorName;
  private String attendanceSummary;
  private String exceptionNote;
  private String content;
  private String sourceOpIdsJson;
  /** draft / published */
  private String status;
}
