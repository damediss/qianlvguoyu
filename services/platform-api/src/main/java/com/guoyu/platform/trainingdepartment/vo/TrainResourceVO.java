package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** 资源视图对象 */
public record TrainResourceVO(
    Long id,
    /** 工种ID */
    Long workTypeId,
    /** 标题 */
    String title,
    /** 类型 */
    String type,
    /** 路径 */
    String path,
    /** 用户ID */
    Long userId,
    /** 排序号 */
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime
) {}
