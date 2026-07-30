package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_candidate_import_batch")
public class CertificationCandidateImportBatchEntity extends BaseEntity {
  private Long taskId;
  private String batchNo;
  private String sourceType;
  private Long sourceFileId;
  private Integer totalCount;
  private Integer successCount;
  private Integer failedCount;
  private Integer duplicateCount;
  private String status;
  private LocalDateTime startedAt;
  private LocalDateTime completedAt;
  private String remark;
}
