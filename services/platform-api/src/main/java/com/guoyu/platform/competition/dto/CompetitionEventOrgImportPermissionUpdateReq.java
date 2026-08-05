package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CompetitionEventOrgImportPermissionUpdateReq(
    @NotNull(message = "请选择名单导入权限")
    @Min(value = 0, message = "名单导入权限必须为0或1")
    @Max(value = 1, message = "名单导入权限必须为0或1")
    Integer importPermission
) {
}
