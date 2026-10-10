package com.guoyu.platform.system.vo;

public record OrgSimpleVO(
    Long id,
    String name,
    String orgType,
    Integer sortOrder
) {}
