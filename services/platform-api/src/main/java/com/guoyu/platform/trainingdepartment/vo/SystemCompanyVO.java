package com.guoyu.platform.trainingdepartment.vo;

public record SystemCompanyVO(
    Long id,
    String code,
    String name,
    Integer sortOrder,
    String status
) {}
