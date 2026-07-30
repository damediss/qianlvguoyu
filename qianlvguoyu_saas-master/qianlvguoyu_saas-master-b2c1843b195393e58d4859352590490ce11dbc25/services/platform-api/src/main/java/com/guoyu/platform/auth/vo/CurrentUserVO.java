package com.guoyu.platform.auth.vo;

import java.util.List;

public record CurrentUserVO(
    Long id,
    Long tenantId,
    String username,
    String displayName,
    String userType,
    List<String> roles,
    List<String> permissions
) {
}
