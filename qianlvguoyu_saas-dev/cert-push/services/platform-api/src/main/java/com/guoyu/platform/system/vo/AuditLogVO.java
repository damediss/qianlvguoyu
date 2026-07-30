package com.guoyu.platform.system.vo;

import java.time.LocalDateTime;

public record AuditLogVO(
    Long id,
    Long tenantId,
    Long userId,
    String module,
    String action,
    String targetType,
    Long targetId,
    String requestId,
    String ip,
    String userAgent,
    String detailJson,
    LocalDateTime createdAt
) {
}
