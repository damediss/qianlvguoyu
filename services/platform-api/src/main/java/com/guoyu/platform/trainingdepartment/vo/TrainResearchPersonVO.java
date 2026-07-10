package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;
import java.util.List;

public record TrainResearchPersonVO(
    Long id,
    Long signUpId,
    String name,
    String idCardNumber,
    String phone,
    String companyName,
    String major,
    String education,
    String workTime,
    String workTypeName,
    String department,
    String job,
    String workDateStart,
    String titleType,
    String titleMajor,
    String titleCertDate,
    Integer level,
    String paperNo,
    String certIssueDates,
    List<TrainResearchPersonCertificateVO> certificates,
    String dataSource,
    LocalDateTime createTime,
    LocalDateTime updateTime
) {}
