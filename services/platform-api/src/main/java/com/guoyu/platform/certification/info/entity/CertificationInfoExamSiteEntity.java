package com.guoyu.platform.certification.info.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_exam_site")
public class CertificationInfoExamSiteEntity extends BaseEntity {
  private String siteName;
  private String siteType;
  private String address;
  private Integer seatCount;
  private String filingNo;
  private String contactName;
  private String contactPhone;
  private String maskedContactPhone;
  private String equipmentCondition;
  private String materialsDescription;
  private String auditStatus;
  private String status;
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
