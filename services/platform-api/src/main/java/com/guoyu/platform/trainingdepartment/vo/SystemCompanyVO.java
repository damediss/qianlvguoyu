package com.guoyu.platform.trainingdepartment.vo;

public record SystemCompanyVO(
    Long id,
    String code,
    String name,
    Long parentId,
    String parentName,
    Integer sortOrder,
    String status
) {}
