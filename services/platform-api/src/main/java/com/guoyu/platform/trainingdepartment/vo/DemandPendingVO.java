package com.guoyu.platform.trainingdepartment.vo;

/** 待办需求列表项（含企业信息） */
public record DemandPendingVO(
    Long id,
    Long orgId,
    String content,
    Integer nums,
    Integer status,
    Integer addtime,
    String addtimeCn,
    String company,
    String companyType
) {}
