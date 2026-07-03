package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_practical_signature_record")
public class PracticalSignatureRecordEntity extends BaseEntity {
  private Long taskId;
  private Long candidateId;
  private Long sessionId;
  private Long signatureFileId;
  private String signatureHash;
  private String signerType;
  private String signerName;
  private LocalDateTime signedAt;
  private String deviceInfo;
  private String recordStatus;
}
