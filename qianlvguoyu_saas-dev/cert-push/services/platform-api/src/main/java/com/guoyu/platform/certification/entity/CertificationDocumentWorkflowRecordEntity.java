package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_document_workflow_record")
public class CertificationDocumentWorkflowRecordEntity extends BaseEntity {

  private Long taskId;
  private Long documentId;
  private String documentType;
  private String processInstanceId;
  private String taskIdInEngine;
  private String nodeKey;
  private String nodeName;
  private String action;
  private String actionResult;
  private String comment;
  private Long operatorUserId;
  private String operatorName;
  private LocalDateTime operatedAt;
}
