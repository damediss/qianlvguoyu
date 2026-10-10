package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_candidate_import_error")
public class CertificationCandidateImportErrorEntity extends BaseEntity {
  private Long batchId;
  private Long taskId;
  private Integer rowNo;
  private String candidateName;
  private String idCardNo;
  private String phone;
  private String errorCode;
  private String errorMessage;
  private String rawJson;
}
