package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record EnterpriseFeatureConfigSaveReq(
    /** 允许的菜单 code；启用约束时生效 */
    @NotNull List<String> menuCodes,
    /** 可授角色 ID */
    @NotNull List<Long> roleIds,
    /** 是否启用功能约束；false 表示该企业不限制（等同未配置） */
    @NotNull Boolean constrainEnabled
) {}
