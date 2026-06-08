package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_archive_import_batch")
public class CertificationArchiveImportBatchEntity extends BaseEntity {
  private Long archiveId;
  private String batchNo;
  private String importType;
  private String zipFileName;
  private String zipFileKey;
  private String status;
  private Integer totalCandidates;
  private Integer successCount;
  private Integer errorCount;
  private String statisticsJson;
}
