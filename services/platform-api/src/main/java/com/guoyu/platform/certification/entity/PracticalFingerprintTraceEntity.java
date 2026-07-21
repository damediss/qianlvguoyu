package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_practical_fingerprint_trace")
public class PracticalFingerprintTraceEntity extends BaseEntity {
  private Long taskId;
  private Long candidateId;
  private Long sessionId;
  private Long examSessionId;
  private String deviceSn;
  private String deviceModel;
  private String deviceVendorId;
  private String deviceProductId;
  private String sdkDeviceType;
  private String adapterType;
  private String collectStatus;
  private BigDecimal qualityScore;
  private Long durationMs;
  private String errorCode;
  private String exceptionReason;
  private Long collectedBy;
  private LocalDateTime collectedAt;
  private Long fingerprintImageFileId;
  private String fingerprintImageHash;
  private String fingerprintImageFormat;
  private LocalDateTime fingerprintImageCapturedAt;
  private String requestId;
}
