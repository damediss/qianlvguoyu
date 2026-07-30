package com.guoyu.platform.trainingdepartment.vo;

/** platform_user 列表项 */
public record PlatformUserVO(
    Long id,
    String realname,
    String idcard,
    String phone,
    Long orgId,
    Long companyId,
    String companyName,
    String addtimeCn
) {}
