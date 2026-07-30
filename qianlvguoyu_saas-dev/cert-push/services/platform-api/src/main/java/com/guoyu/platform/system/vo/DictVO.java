package com.guoyu.platform.system.vo;

import java.time.LocalDateTime;

public record DictVO(
    Long id,
    Long tenantId,
    String code,
    String name,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
