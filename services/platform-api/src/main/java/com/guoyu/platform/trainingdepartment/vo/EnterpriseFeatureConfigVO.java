package com.guoyu.platform.trainingdepartment.vo;

import java.util.List;

public record EnterpriseFeatureConfigVO(
    Long enterpriseId,
    String enterpriseName,
    Long configurerOrgId,
    List<String> menuCodes,
    List<Long> roleIds,
    boolean constrainEnabled,
    boolean configured
) {}
