package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 开班人员列表视图对象 */
public record TrainPeopleListVO(
    Long id,
    Long trainId,
    Long trainClassId,
    String type,
    Long userId,
    Long platformUserId,
    String realname,
    String phone,
    Integer isLeader,
    Integer isCommittee,
    BigDecimal examScore,
    BigDecimal realScore,
    String paper,
    BigDecimal normalScore,
    String nationalPaper,
    Integer currentLevel,
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime,
    String idCard
) {}
