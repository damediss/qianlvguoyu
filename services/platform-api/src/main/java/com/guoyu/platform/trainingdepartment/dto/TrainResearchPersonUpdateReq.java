package com.guoyu.platform.trainingdepartment.dto;

import java.util.List;

public record TrainResearchPersonUpdateReq(
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
    List<TrainResearchPersonCertificateReq> certificates
) {}
