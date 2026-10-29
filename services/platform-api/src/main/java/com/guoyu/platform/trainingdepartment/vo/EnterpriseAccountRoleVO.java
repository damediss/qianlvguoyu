package com.guoyu.platform.trainingdepartment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record EnterpriseAccountRoleVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    String code,
    String name,
    boolean defaultRole
) {}
