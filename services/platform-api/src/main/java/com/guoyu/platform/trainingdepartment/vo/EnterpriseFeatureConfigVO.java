package com.guoyu.platform.trainingdepartment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;

public record EnterpriseFeatureConfigVO(
    @JsonSerialize(using = ToStringSerializer.class) Long enterpriseId,
    String enterpriseName,
    @JsonSerialize(using = ToStringSerializer.class) Long configurerOrgId,
    List<String> menuCodes,
    @JsonSerialize(contentUsing = ToStringSerializer.class) List<Long> roleIds,
    boolean constrainEnabled,
    boolean configured
) {}
