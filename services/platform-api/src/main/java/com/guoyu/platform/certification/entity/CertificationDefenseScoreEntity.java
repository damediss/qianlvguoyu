package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_defense_score")
public class CertificationDefenseScoreEntity extends BaseEntity {
  private Long taskId;
  private Long candidateId;
  private Long reportId;
  private Long assessorId;
  private Long assessorUserId;
  private String sheetType;
  private String status;
  private String payloadJson;
  private BigDecimal contentSubtotal;
  private BigDecimal defenseSubtotal;
  private BigDecimal rawTotal;
  private BigDecimal finalScore;
  private Integer rejected;
  private Long signatureFileId;
  private String signatureHash;
  private LocalDateTime submittedAt;
}
