package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** 用户证书视图对象 */
public record TrainUserPaperVO(
    Long id,
    /** 证书链接 */
    String url,
    /** 证书标题 */
    String title,
    /** 证书类型 */
    String type,
    /** 用户ID */
    Long userId,
    /** 平台用户ID */
    Long platformUserId,
    /** 排序号 */
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime,
    /** 关联主键 */
    Long pk
) {}
