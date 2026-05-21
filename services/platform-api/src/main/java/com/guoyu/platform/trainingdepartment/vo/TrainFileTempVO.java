package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** train_file_temp 视图对象 */
public record TrainFileTempVO(
    Long id,
    /** 模板类型 */
    String type,
    /** 模板名称 */
    String title,
    /** 城市/地区 */
    String city,
    /** 源文件路径 */
    String pathSource,
    /** 租户ID */
    Long tenantId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
