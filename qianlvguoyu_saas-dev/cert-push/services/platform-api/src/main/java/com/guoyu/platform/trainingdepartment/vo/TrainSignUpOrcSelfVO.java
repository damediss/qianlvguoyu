package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** train_sign_up_orc_self 视图对象 */
public record TrainSignUpOrcSelfVO(
    Long id,
    /** 报名计划ID */
    Long signUpId,
    /** 平台用户ID */
    Long platformUserId,
    /** OCR识别原始数据 */
    String orcData,
    /** 状态 0-失败 1-成功 */
    Integer status,
    /** 失败原因 */
    String failReason,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
