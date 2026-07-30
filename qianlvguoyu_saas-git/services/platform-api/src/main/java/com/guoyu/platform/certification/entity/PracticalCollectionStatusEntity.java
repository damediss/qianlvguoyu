package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_practical_collection_status")
public class PracticalCollectionStatusEntity extends BaseEntity {
  private Long taskId;
  private Long candidateId;
  private Long sessionId;
  private Long examSessionId;
  private String collectToken;
  private String qrIssueStatus;
  private String overallStatus;
  private String queueStatus;
  private String fingerprintStatus;
  private String signatureStatus;
  private String exceptionStatus;
  private String matchMethod;
  private Integer queueNo;
  private String idCardLast4;
  private LocalDateTime tokenGeneratedAt;
  private LocalDateTime qrExportedAt;
  private LocalDateTime qrIssuedAt;
  private LocalDateTime arrivedAt;
  private LocalDateTime collectingStartedAt;
  private LocalDateTime completedAt;
  private Long lastOperatorId;
  private String remark;
}
