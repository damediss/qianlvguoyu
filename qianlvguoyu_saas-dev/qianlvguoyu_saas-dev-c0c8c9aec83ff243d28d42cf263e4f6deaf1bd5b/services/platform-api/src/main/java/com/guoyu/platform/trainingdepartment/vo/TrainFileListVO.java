package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** 文件列表视图对象 */
public record TrainFileListVO(
    Long id,
    /** 表名 */
    String tableName,
    /** 关联主键 */
    String pk,
    /** 标题 */
    String title,
    /** 用户ID */
    Long userId,
    /** 状态 */
    Integer status,
    /** 排序号 */
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime
) {}
