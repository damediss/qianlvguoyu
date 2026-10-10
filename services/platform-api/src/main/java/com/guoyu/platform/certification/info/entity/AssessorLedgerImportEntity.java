package com.guoyu.platform.certification.info.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_assessor_ledger_import")
public class AssessorLedgerImportEntity extends BaseEntity {
  private Long sourceFileId;
  private String status;
  private Integer totalCount;
  private Integer importedCount;
  private Integer skippedCount;
  private Integer failedCount;
  private String resultMessage;
  private Long operatorId;
  private LocalDateTime startedAt;
  private LocalDateTime completedAt;
}
