package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 编辑企业账号（登录账号不可改） */
public record EnterpriseAccountUpdateReq(
    @NotBlank @Size(max = 64) String displayName,
    @Size(max = 32) String phone,
    @Size(max = 128) String email,
    @NotBlank @Size(max = 32) String status,
    @NotNull List<Long> roleIds
) {}
