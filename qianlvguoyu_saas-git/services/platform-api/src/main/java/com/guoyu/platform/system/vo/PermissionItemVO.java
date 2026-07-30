package com.guoyu.platform.system.vo;

import java.util.List;

public record PermissionItemVO(
    Long id,
    String code,
    String name,
    String action,
    String status,
    String menuName,
    List<RoleBriefVO> boundRoles
) {
}
