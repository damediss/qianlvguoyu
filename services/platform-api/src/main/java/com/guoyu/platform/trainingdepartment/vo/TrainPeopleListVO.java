package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 开班人员列表视图对象 */
public record TrainPeopleListVO(
    Long id,
    /** 培训ID */
    Long trainId,
    /** 培训班级ID */
    Long trainClassId,
    /** 人员类型 */
    String type,
    /** 用户ID */
    Long userId,
    /** 平台用户ID */
    Long platformUserId,
    /** 真实姓名 */
    String realname,
    /** 电话 */
    String phone,
    /** 是否组长 */
    Integer isLeader,
    /** 是否班委 */
    Integer isCommittee,
    /** 考试成绩 */
    BigDecimal examScore,
    /** 实操成绩 */
    BigDecimal realScore,
    /** 证书 */
    String paper,
    /** 平时成绩 */
    BigDecimal normalScore,
    /** 国网证书 */
    String nationalPaper,
    /** 当前等级 */
    Integer currentLevel,
    /** 排序号 */
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime,
    /** 身份证 */
    String idCard,
    /** 审核状态: 0待审核, 1通过, 2拒绝 */
    Integer status
) {}
