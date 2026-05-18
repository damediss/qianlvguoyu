package com.guoyu.platform.trainingdepartment.vo;

/** 平台用户视图对象 */
public record PlatformUserVO(
    Long id,
    /** 真实姓名 */
    String realname,
    /** 身份证号 */
    String idcard,
    /** 手机号 */
    String phone,
    /** 状态 */
    String stat,
    /** 添加时间(Unix时间戳) */
    Long addtime,
    /** 最后登录时间(Unix时间戳) */
    Long logintime,
    /** 机构ID */
    Long orgId,
    /** 企业ID */
    Long companyId,
    /** 微信OpenID */
    String wxopenid,
    /** 专家ID */
    Long expertId,
    /** 任务标识 */
    Integer task
) {}
