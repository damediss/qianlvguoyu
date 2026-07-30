package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TrainExamPeopleListVO(
    Long id,
    Long examId,
    Long platformUserId,
    Long workTypeId,
    String workTypeLevel,
    String realname,
    String idCardNum,
    String phone,
    String education,
    Long roomId,
    Integer inWhiteList,
    BigDecimal normalScore,
    BigDecimal score,
    BigDecimal totalScore,
    Integer learnTime,
    Integer learnUseTime,
    Long userId,
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime,
    LocalDateTime updateTime
) {}
