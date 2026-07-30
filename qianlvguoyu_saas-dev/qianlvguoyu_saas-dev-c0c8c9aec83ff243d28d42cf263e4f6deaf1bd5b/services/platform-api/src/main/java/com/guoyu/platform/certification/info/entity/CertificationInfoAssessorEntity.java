package com.guoyu.platform.certification.info.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_assessor")
public class CertificationInfoAssessorEntity extends BaseEntity {
  private Long userId;
  private String assessorName;
  private String certificateNo;
  private String workTypeScope;
  private String workTypeJson;
  private String levelScope;
  private String levelJson;
  private LocalDate validUntil;
  private String phone;
  private String maskedPhone;
  private Long orgId;
  private String orgName;
  private String materialsDescription;
  private String auditStatus;
  private String workStatus;
  private LocalDateTime submittedAt;
  private Long submittedBy;
  private LocalDateTime reviewedAt;
  private Long reviewedBy;
  private String reviewerName;
  private String rejectReason;
  private LocalDateTime disabledAt;
  private Long disabledBy;
  private String disableReason;
  private Integer referencedCount;
  private String remark;
}
