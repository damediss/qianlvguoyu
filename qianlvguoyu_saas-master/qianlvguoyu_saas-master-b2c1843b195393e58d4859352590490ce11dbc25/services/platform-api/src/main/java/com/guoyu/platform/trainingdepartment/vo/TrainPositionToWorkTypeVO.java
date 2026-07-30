package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** train_position_to_work_type 视图对象 */
public record TrainPositionToWorkTypeVO(
    Long id,
    /** 岗位名称 */
    String positionName,
    /** 工种ID */
    Long workTypeId,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
