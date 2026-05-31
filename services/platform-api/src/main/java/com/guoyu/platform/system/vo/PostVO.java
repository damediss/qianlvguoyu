package com.guoyu.platform.system.vo;

import java.time.LocalDateTime;

public record PostVO(
    Long id,
    Long tenantId,
    String code,
    String name,
    Integer sortOrder,
    String status,
    String remark,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
