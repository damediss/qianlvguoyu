package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 课表调课申请 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_schedule_change_request")
public class TrainScheduleChangeRequestEntity extends BaseEntity {

  private Long classId;
  private Long scheduleId;
  /** reschedule / replace_teacher / cancel / add */
  private String changeType;
  private String beforeSnapshot;
  private String afterPayload;
  private String reason;
  /** pending / approved / rejected / cancelled */
  private String status;
  private Long applicantPlatformUserId;
  private String applicantRoleCode;
  private Long reviewerUserId;
  private LocalDateTime reviewedAt;
  private String rejectReason;
}
