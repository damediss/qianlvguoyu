package com.guoyu.platform.certification.info.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_info_exam_staff")
public class CertificationInfoExamStaffEntity extends BaseEntity {
  private Long userId;
  private String staffName;
  private String accountUsername;
  private String phone;
  private String maskedPhone;
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
}
