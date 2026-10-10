package com.guoyu.platform.certification.vo;

import java.util.List;

public record CertificationDocumentWorkflowVO(
    String documentId,
    String documentType,
    String documentStatus,
    String processInstanceId,
    String currentNodeKey,
    String currentNodeName,
    List<CertificationDocumentWorkflowRecordVO> records
) {
}
