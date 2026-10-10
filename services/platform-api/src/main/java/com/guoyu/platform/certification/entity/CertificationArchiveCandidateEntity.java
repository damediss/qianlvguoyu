package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_archive_candidate")
public class CertificationArchiveCandidateEntity extends BaseEntity {
  private Long archiveId;
  private Long sourceBatchId;
  private String candidateNo;
  private String name;
  private String idCard;
  private String maskedIdCard;
  private String folderName;
}
