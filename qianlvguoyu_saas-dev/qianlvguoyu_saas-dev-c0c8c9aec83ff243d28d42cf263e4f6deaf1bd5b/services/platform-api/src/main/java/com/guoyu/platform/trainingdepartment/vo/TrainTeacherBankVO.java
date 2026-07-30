package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;

/** 师资库视图对象 */
public record TrainTeacherBankVO(
    Long id,
    /** 姓名 */
    String name,
    /** 电话 */
    String phone,
    /** 身份证 */
    String idCard,
    /** 教师资格证 */
    String teacherLicense,
    /** 工程师证 */
    String engineerLicense,
    /** 等级证书 */
    String levelLicense,
    /** 学历证书 */
    String eduLicense,
    /** 毕业证书 */
    String graduatePaper,
    /** 公司 */
    String company,
    /** 地址 */
    String address,
    /** 身份证号 */
    String idCardNumber,
    /** 开始日期 */
    String startDate,
    /** 结束日期 */
    String endDate,
    /** 生日 */
    String birthday,
    /** 性别 */
    Integer gender,
    /** 民族 */
    String nation,
    /** 学历 */
    String education,
    /** 专业 */
    String major,
    /** 状态 */
    Integer status,
    Long tenantId,
    /** 用户ID */
    Long userId,
    /** 排序号 */
    Integer sortNumber,
    LocalDateTime createTime,
    /** 更新时间 */
    LocalDateTime updateTime
) {}
