package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationDocumentWorkflowRecordEntity;
import java.time.LocalDateTime;

public record CertificationDocumentWorkflowRecordVO(
    String id,
    String processInstanceId,
    String engineTaskId,
    String nodeKey,
    String nodeName,
    String action,
    String actionResult,
    String comment,
    String operatorUserId,
    String operatorName,
    LocalDateTime operatedAt
) {

  public static CertificationDocumentWorkflowRecordVO from(CertificationDocumentWorkflowRecordEntity entity) {
    return new CertificationDocumentWorkflowRecordVO(
        entity.getId() == null ? null : String.valueOf(entity.getId()),
        entity.getProcessInstanceId(),
        entity.getTaskIdInEngine(),
        entity.getNodeKey(),
        entity.getNodeName(),
        entity.getAction(),
        entity.getActionResult(),
        entity.getComment(),
        entity.getOperatorUserId() == null ? null : String.valueOf(entity.getOperatorUserId()),
        entity.getOperatorName(),
        entity.getOperatedAt()
    );
  }
}
