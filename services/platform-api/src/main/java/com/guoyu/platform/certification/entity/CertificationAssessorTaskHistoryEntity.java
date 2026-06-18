package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_assessor_task_history")
public class CertificationAssessorTaskHistoryEntity extends BaseEntity {
  private Long taskId;
  private Long assessorTaskId;
  private Long candidateId;
  private Long paperQuestionId;
  private Long assessorId;
  private Integer revisionNo;
  private String action;
  private Integer versionNo;
  private BigDecimal totalScore;
  private String snapshotJson;
  private String reason;
  private LocalDateTime operatedAt;
  private Long operatedBy;
}
