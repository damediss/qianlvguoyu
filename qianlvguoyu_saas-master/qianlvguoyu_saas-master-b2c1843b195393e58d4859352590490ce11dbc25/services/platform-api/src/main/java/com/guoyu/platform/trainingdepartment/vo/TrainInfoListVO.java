package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 开班信息视图对象 */
public record TrainInfoListVO(
    Long id,
    /** 标题 */
    String title,
    /** 封面 */
    String cover,
    /** 简介 */
    String intro,
    /** 班组类型 */
    Integer groupType,
    /** 师傅类型 */
    Integer masterType,
    /** 类型 */
    Integer type,
    /** 分类 */
    String cate,
    /** 企业ID */
    Long companyId,
    /** 企业名称 */
    String companyName,
    /** 企业地址 */
    String companyAddress,
    /** 工种ID */
    Long workTypeId,
    /** 工种等级 */
    Integer workTypeLevel,
    /** 课时ID */
    Long courseId,
    /** 人数 */
    Integer num,
    /** 岗位 */
    String position,
    /** 课时数 */
    Integer courseNum,
    /** 负责人确认 */
    Integer headConfirm,
    /** 用户ID */
    Long userId,
    /** 企业是否确认 */
    Integer companyHasConfirm,
    /** 企业确认时间 */
    LocalDateTime companyConfirmTime,
    /** 企业确认人ID */
    Long companyConfirmUserId,
    /** 企业确认人名称 */
    String companyConfirmUsername,
    /** 通过状态 */
    Integer passStatus,
    /** 审核人ID */
    Long checkUserId,
    /** 审核人名称 */
    String checkUsername,
    /** 是否顺序学习 */
    Integer isSortedLearn,
    /** 最大平时分 */
    BigDecimal maxNormalScore,
    /** 课时文件 */
    String courseFile,
    /** 开始日期 */
    String startDate,
    /** 结束日期 */
    String endDate,
    /** 培训企业名称 */
    String trainCompanyName,
    /** 状态 */
    Integer status,
    /** 排序号 */
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime,
    /** 积分 */
    Integer point,
    /** 相册 */
    String album,
    /** 总结 */
    String summary,
    /** 省ID */
    Long provinceId,
    /** 省名称 */
    String provinceName,
    /** 市ID */
    Long cityId,
    /** 市名称 */
    String cityName,
    /** 能力分 */
    BigDecimal abilityScore,
    /** 区ID */
    Long regionId,
    /** 区名称 */
    String regionName,
    /** 负责人ID */
    Long inChargeUserId,
    /** 负责人名称 */
    String inChargeUsername,
    /** 附件 */
    String files,
    /** 培训需求ID */
    Long traindemandId
) {}
