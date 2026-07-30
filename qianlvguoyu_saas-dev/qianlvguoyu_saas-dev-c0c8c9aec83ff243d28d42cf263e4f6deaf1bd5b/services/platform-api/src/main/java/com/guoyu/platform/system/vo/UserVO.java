package com.guoyu.platform.system.vo;

import java.time.LocalDateTime;
import java.util.List;

public record UserVO(
    Long id,
    Long tenantId,
    Long orgId,
    String username,
    String displayName,
    String userType,
    String phone,
    String email,
    String status,
    LocalDateTime lastLoginAt,
    List<Long> roleIds,
    List<String> roleCodes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
