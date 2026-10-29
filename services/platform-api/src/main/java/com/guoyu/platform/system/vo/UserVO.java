package com.guoyu.platform.system.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.List;

public record UserVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long tenantId,
    @JsonSerialize(using = ToStringSerializer.class) Long orgId,
    @JsonSerialize(using = ToStringSerializer.class) Long postId,
    String postName,
    String username,
    String displayName,
    String userType,
    String phone,
    String email,
    String status,
    LocalDateTime lastLoginAt,
    @JsonSerialize(contentUsing = ToStringSerializer.class) List<Long> roleIds,
    List<String> roleCodes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
