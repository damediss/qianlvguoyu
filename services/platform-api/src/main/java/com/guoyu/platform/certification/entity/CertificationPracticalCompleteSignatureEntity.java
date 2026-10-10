package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_practical_complete_signature")
public class CertificationPracticalCompleteSignatureEntity extends BaseEntity {
  private Long sessionId;
  private Long taskId;
  private Long groupId;
  private Long assessorId;
  private String assessorName;
  private String status;
  private Long signatureFileId;
  private String signatureHash;
  private LocalDateTime signedAt;
  private String deviceInfo;
}
