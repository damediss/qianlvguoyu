package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SystemCompanyCreateReq(
    @NotBlank(message = "企业名称不能为空")
    @Size(max = 128, message = "企业名称过长")
    String name,
    Integer sortOrder,
    @Size(max = 32) String status
) {}
