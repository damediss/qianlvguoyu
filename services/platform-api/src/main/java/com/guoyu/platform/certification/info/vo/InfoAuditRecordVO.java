package com.guoyu.platform.certification.info.vo;

import java.time.LocalDateTime;

public record InfoAuditRecordVO(
    Long id,
    String ownerType,
    Long ownerId,
    String action,
    String beforeStatus,
    String afterStatus,
    Long operatorId,
    String operatorName,
    String comment,
    LocalDateTime createdAt
) {
}
