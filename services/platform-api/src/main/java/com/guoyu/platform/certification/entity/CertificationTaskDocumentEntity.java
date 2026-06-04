package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_task_document")
public class CertificationTaskDocumentEntity extends BaseEntity {

  private Long taskId;
  private String documentType;
  private String documentName;
  private Integer versionNo;
  private Integer isCurrent;
  private String status;
  private Integer locked;
  private String contentText;
  private String contentJson;
  private Integer completionPercent;
  private Integer requiredValid;
  private LocalDateTime submittedAt;
  private Long submittedBy;
  private LocalDateTime approvedAt;
  private Long approvedBy;
  private LocalDateTime rejectedAt;
  private Long rejectedBy;
  private String rejectReason;
  private String processInstanceId;
  private LocalDateTime lastSavedAt;
  private String remark;
}
