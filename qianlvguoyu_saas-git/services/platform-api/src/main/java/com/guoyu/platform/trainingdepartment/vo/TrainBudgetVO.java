package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 培训预算视图对象
 */
public record TrainBudgetVO(
    /** 主键ID */
    Long id,
    /** 培训ID */
    Long trainId,
    String province,
    String city,
    String region,
    /** 组织ID */
    Long orgId,
    /** 组织名称 */
    String orgName,
    /** 培训日期 */
    String trainDate,
    Long trainWorkTypeId,
    String trainWorkTypeName,
    String trainLevel,
    String bonusStandard,
    BigDecimal expectIncome,
    BigDecimal actualIncome,
    BigDecimal schoolManageFee,
    BigDecimal manageFee,
    BigDecimal researchFee,
    BigDecimal companyRequireCommission,
    BigDecimal agentRequireCommission,
    BigDecimal teacherCourseFee,
    BigDecimal teacherFee,
    BigDecimal staffFee,
    BigDecimal staffTravelFee,
    BigDecimal trainFee,
    BigDecimal pointRewards,
    BigDecimal businessFee,
    Integer peopleNum,
    String chargeFee,
    BigDecimal serviceFee,
    BigDecimal reviewFee,
    BigDecimal maintainFee,
    Integer makeupPeopleNum,
    Integer theoryMakeupPeopleNum,
    Integer operationMakeupPeopleNum,
    BigDecimal staffMakeupTrainFee,
    BigDecimal reviewMakeupTrainFee,
    BigDecimal profit,
    String comments,
    /** 状态 */
    Integer status,
    /** 排序号 */
    Integer sortNumber,
    /** 租户ID */
    Long tenantId,
    /** 用户ID */
    Long userId,
    /** 创建时间 */
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
