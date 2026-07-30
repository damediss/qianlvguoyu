package com.guoyu.platform.system.vo;

import java.time.LocalDateTime;
import java.util.List;

public record RoleVO(
    Long id,
    Long tenantId,
    String code,
    String name,
    String roleType,
    String status,
    String remark,
    List<Long> menuIds,
    List<Long> permissionIds,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
