package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_assessor_notification")
public class CertificationAssessorNotificationEntity extends BaseEntity {
  private Long taskId;
  private Long assessorId;
  private Long userId;
  private String messageType;
  private String messageContent;
  private String readStatus;
  private LocalDateTime readAt;
}
