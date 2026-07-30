package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_archive_candidate_material")
public class CertificationArchiveCandidateMaterialEntity extends BaseEntity {
  private Long archiveCandidateId;
  private Long sourceBatchId;
  private String materialType;
  private String materialGroup;
  private String fileName;
  private String fileKey;
  private Long fileSize;
  private String fileHash;
  private String contentType;
}
