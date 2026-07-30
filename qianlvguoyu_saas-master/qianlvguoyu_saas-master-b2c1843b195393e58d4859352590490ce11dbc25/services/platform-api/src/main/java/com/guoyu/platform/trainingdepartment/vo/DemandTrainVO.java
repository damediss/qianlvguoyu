package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** demand_train 视图对象 */
public record DemandTrainVO(
    Long id,
    /** 企业ID */
    Long orgId,
    /** 需求内容 */
    String content,
    /** 人数 */
    Integer nums,
    /** 状态 0-待处理 1-已处理 */
    Integer status,
    /** 添加时间戳 */
    Integer addtime,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
