package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_risk")
public class CertificationRiskEntity extends BaseEntity {

  private Long taskId;
  private Long documentId;
  private String riskNo;
  private String riskDescription;
  private String probabilityCode;
  private String impactCode;
  private String riskLevelCode;
  private Integer riskLevelScore;
  private String responsePlan;
  private Long ownerMemberId;
  private Long ownerUserId;
  private String ownerName;
  private String status;
  private LocalDate expectedCloseDate;
  private LocalDateTime closedAt;
  private Long closedBy;
  private String closeReason;
  private Integer sortOrder;
  private String remark;
}
