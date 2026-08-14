package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_follow_supervision")
public class TrainFollowSupervisionEntity extends BaseEntity {
  private Long classId;
  private String bizDate;
  private Long scheduleId;
  private Long inspectorUserId;
  private String inspectorName;
  /** draft / submitted / closed */
  private String status;
  /** pass / fail / conditional */
  private String conclusion;
  private String remark;
  private LocalDateTime submittedAt;
}
