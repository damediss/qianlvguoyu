package com.guoyu.platform.system.vo;

import java.time.LocalDateTime;

public record DictItemVO(
    Long id,
    Long tenantId,
    Long dictId,
    String itemValue,
    String itemLabel,
    Integer sortOrder,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
