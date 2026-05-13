package com.guoyu.platform.system.vo;

import java.time.LocalDateTime;

public record TenantVO(
    Long id,
    String code,
    String name,
    String status,
    String contactName,
    String contactPhone,
    String remark,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
