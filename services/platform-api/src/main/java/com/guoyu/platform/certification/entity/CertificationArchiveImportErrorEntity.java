package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_archive_import_error")
public class CertificationArchiveImportErrorEntity extends BaseEntity {
  private Long batchId;
  private String candidateNo;
  private String name;
  private String idCard;
  private String errorType;
  private String errorMessage;
}
