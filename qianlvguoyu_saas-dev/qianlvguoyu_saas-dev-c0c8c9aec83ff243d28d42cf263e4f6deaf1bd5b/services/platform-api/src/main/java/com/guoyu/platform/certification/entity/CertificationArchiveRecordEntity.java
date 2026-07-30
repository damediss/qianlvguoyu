package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_archive_record")
public class CertificationArchiveRecordEntity extends BaseEntity {
  private Long taskId;
  private String taskNo;
  private String taskName;
  private String batchName;
  private String workTypeName;
  private String levelName;
  private LocalDateTime completedAt;
  private Integer candidateCount;
  private String archiveStatus;
  private Boolean basicArchived;
  private Boolean materialArchived;
  private Long archivedBy;
  private LocalDateTime archivedAt;
  private Long lastOperatorId;
  private String lastOperatorName;
}
