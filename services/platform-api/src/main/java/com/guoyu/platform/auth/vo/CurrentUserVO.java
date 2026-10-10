package com.guoyu.platform.auth.vo;

import java.util.List;

public record CurrentUserVO(
    Long id,
    Long tenantId,
    String username,
    String displayName,
    String userType,
    Long orgId,
    String orgType,
    /** 对齐 yun_shang：org=培训机构可改班，company=企业仅看本企业班级 */
    String loginRole,
    List<String> roles,
    List<String> permissions
) {
}
