package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** train_base_data 视图对象 */
public record TrainBaseDataVO(
    Long id,
    /** 配置键 */
    String dataKey,
    /** 配置值 */
    String dataValue,
    /** 数据类型 */
    String dataType,
    /** 描述 */
    String description,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
