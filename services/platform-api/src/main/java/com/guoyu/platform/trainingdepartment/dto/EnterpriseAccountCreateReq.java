package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 为企业开设管理端登录账号 */
public record EnterpriseAccountCreateReq(
    @NotBlank @Size(max = 64) String username,
    @NotBlank @Size(min = 8, max = 128) String password,
    @NotBlank @Size(max = 64) String displayName,
    @Size(max = 32) String phone,
    @Size(max = 128) String email,
    @Size(max = 32) String status,
    /** 为空时自动绑定 TRAIN_ENTERPRISE 角色 */
    List<Long> roleIds
) {}
