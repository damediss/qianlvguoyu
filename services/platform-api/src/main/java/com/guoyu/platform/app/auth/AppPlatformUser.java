package com.guoyu.platform.app.auth;

/** App 端 platform_user 登录上下文（istate token 解析结果） */
public record AppPlatformUser(
    Long id,
    String realname,
    String phone,
    String idcard,
    Long expertId
) {
}
