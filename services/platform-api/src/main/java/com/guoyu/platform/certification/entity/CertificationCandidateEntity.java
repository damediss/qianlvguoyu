package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_candidate")
public class CertificationCandidateEntity extends BaseEntity {
  private Long taskId;
  private Long taskScopeId;
  private String candidateNo;
  private String candidateName;
  private String idCardNo;
  private String maskedIdCardNo;
  private String phone;
  private String maskedPhone;
  private String levelCode;
  private String levelName;
  private Long workTypeId;
  private String workTypeName;
  private Long importBatchId;
  private String importSource;
  private String orgName;
  private Integer documentCompletedCount;
  private Integer documentRequiredCount;
  private Boolean documentComplete;
  private String status;
  private String remark;
}
