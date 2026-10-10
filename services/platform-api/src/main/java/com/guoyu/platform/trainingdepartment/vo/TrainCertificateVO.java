package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/**
 * 培训证书视图对象
 */
public record TrainCertificateVO(
    /** 主键ID */
    Long id,
    /** 项目ID */
    Long projectId,
    /** 用户ID */
    Long userId,
    String userName,
    /** 证书编号 */
    String certificateNo,
    String certificateType,
    /** 工种 */
    String workType,
    /** 等级 */
    String level,
    /** 颁发日期 */
    String issueDate,
    String expireDate,
    String issuingAuthority,
    String filePath,
    /** 状态 */
    Integer status,
    /** 排序号 */
    Integer sortNumber,
    /** 租户ID */
    Long tenantId,
    Long createdBy,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
