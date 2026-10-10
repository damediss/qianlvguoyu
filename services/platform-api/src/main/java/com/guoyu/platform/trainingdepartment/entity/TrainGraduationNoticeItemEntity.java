package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_graduation_notice_item")
public class TrainGraduationNoticeItemEntity extends BaseEntity {
  private Long noticeId;
  private Long peopleId;
  private Long platformUserId;
  private String phone;
  private String status;
  private String failReason;
  private LocalDateTime readAt;
}
