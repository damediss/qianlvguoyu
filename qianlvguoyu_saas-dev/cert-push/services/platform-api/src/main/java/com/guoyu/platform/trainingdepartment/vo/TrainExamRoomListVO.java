package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** train_exam_room_list 视图对象 */
public record TrainExamRoomListVO(
    Long id,
    /** 组织ID */
    Long orgId,
    /** 组织名称 */
    String orgName,
    /** 考场类型 */
    String type,
    /** 考场名称 */
    String roomName,
    /** 位置 */
    String position,
    /** 最大容纳人数 */
    Integer maxNum,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
