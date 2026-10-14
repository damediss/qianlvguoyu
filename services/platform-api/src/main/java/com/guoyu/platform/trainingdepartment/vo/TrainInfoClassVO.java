package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;

/** 培训班级视图对象 */
public record TrainInfoClassVO(
    Long id,
    /** 开班信息ID */
    Long trainInfoId,
    /** 班级标题 */
    String title,
    /** 人数 */
    Integer nums,
    /** 达标课时 */
    BigDecimal passHours,
    /** 企业ID */
    Long companyId,
    /** 工种ID */
    Long workTypeId,
    /** 开始日期 */
    String dateStart,
    /** 结束日期 */
    String dateEnd,
    /** 添加时间 */
    Integer addtime,
    /** 文件模板 */
    String fileTemps,
    /** 课表文件 */
    String kcbiaoFile,
    /** 单位联系人 */
    String companyContactName,
    /** 单位联系方式 */
    String companyContactPhone
) {}
