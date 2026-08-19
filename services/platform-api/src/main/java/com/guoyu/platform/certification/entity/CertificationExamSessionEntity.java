package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_exam_session")
public class CertificationExamSessionEntity extends BaseEntity {
  private Long taskId;
  private Long taskScopeId;
  private String examType;
  private String sessionName;
  private LocalDate examDate;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private Long examSiteId;
  private String examVenue;
  private String status;
  private Boolean identityCheckRequired;
  private Boolean fingerprintRequired;
  private Boolean signatureRequired;
  private String remark;
}
