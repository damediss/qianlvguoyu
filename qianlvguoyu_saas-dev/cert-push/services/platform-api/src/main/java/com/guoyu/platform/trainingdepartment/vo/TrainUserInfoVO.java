package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** 参与培训人员信息视图对象 */
public record TrainUserInfoVO(
    Long id,
    /** 用户ID */
    Long userId,
    /** 企业ID */
    Long companyId,
    /** 人员类型 */
    String personType,
    /** 人员补充信息 */
    String personSupplementary,
    /** 住房类型 */
    String houseType,
    /** 国籍 */
    String nationality,
    /** 学历 */
    String education,
    /** 求职意向 */
    String jobIntention,
    /** 是否党员 */
    Integer isPartyMember,
    /** 地址 */
    String address,
    /** 身份证正面 */
    String idCardFront,
    /** 身份证反面 */
    String idCardBack,
    /** 照片 */
    String photo,
    /** 学历照片 */
    String eduPhoto,
    /** 工作合同 */
    String workContract,
    /** 排序号 */
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime
) {}
