package com.guoyu.platform.system.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RolePermissionBindReq(
    @NotNull List<Long> menuIds,
    @NotNull List<Long> permissionIds
) {
}
