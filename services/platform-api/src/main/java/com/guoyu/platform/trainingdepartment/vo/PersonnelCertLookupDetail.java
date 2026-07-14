package com.guoyu.platform.trainingdepartment.vo;

public record PersonnelCertLookupDetail(
    Integer rowIndex,
    String name,
    String idCard,
    String status,
    String certLevel,
    String certNo,
    String issueDate,
    String message
) {}
