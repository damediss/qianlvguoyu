package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_practical_complete_sign_session")
public class CertificationPracticalCompleteSignSessionEntity extends BaseEntity {
  private Long taskId;
  private String status;
  private LocalDateTime requestedAt;
  private Long requestedBy;
  private Integer requiredCount;
  private Integer signedCount;
  private LocalDateTime completedAt;
}
