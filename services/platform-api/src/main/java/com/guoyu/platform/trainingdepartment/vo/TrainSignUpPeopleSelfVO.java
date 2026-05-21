package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** train_sign_up_people_self 视图对象 */
public record TrainSignUpPeopleSelfVO(
    Long id,
    /** 报名计划ID */
    Long signUpId,
    /** 平台用户ID */
    Long platformUserId,
    /** 姓名 */
    String name,
    /** 手机号 */
    String phone,
    /** 身份证号 */
    String idCardNumber,
    /** 性别 */
    String gender,
    /** 学历 */
    String education,
    /** 照片路径 */
    String photo,
    /** 合同路径 */
    String contract,
    /** 证书路径 */
    String certificate,
    /** 状态 */
    Integer status,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
