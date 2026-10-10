package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_assessor_task")
public class CertificationAssessorTaskEntity extends BaseEntity {
  private Long taskId;
  private Long groupId;
  private Long taskAssessorId;
  private Long assessorId;
  private Long leaderAssessorId;
  private Long candidateId;
  private Long paperId;
  private Long paperQuestionId;
  private Integer revisionNo;
  private Integer versionNo;
  private String candidateSnapshotJson;
  private String questionSnapshotJson;
  private String questionId;
  private String subject;
  private String title;
  private Long assessmentFileId;
  private String assessmentFileName;
  private Integer durationMin;
  private LocalDate dueDate;
  private String status;
  private String progressNote;
  private BigDecimal totalScore;
  private LocalDateTime submittedAt;
  private LocalDateTime returnedAt;
  private Long returnedBy;
  private String returnReason;
  private LocalDateTime reviewedAt;
  private Long reviewedBy;
  private String reviewNote;
  private String remark;
}
