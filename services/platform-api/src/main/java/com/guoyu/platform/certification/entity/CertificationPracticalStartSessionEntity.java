package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_practical_start_session")
public class CertificationPracticalStartSessionEntity extends BaseEntity {
  private Long taskId;
  private Long candidateId;
  private Long configId;
  private Long paperId;
  private Long groupId;
  private LocalDateTime candidateConfirmedAt;
  private Long candidateConfirmedBy;
  private String assessorConfirmationsJson;
  private String status;
  private Integer versionNo;
  private LocalDateTime startedAt;
  private Long startedBy;
  private LocalDateTime completedAt;
  private Long completedBy;
  private LocalDateTime summaryGeneratedAt;
  private String summaryFileName;
}
