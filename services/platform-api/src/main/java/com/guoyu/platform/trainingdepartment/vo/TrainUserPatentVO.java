package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** 用户专利视图对象 */
public record TrainUserPatentVO(
    Long id,
    /** 专利标题 */
    String title,
    /** 专利证书 */
    String paper,
    /** 用户ID */
    Long userId,
    Long tenantId,
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
