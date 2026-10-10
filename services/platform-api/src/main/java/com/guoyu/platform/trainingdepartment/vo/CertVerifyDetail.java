package com.guoyu.platform.trainingdepartment.vo;

public record CertVerifyDetail(
    Long peopleId,
    String name,
    String idCardNumber,
    boolean found,
    String certLevel,
    String certNo,
    String issueDate,
    String errorMessage
) {}
