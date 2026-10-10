package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** 文件详情视图对象 */
public record TrainFileDetailVO(
    Long id,
    /** 文件ID */
    Long fileId,
    /** 标题 */
    String title,
    /** 路径 */
    String path,
    /** 用户ID */
    Long userId,
    /** 状态 */
    Integer status,
    /** 排序号 */
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime
) {}
