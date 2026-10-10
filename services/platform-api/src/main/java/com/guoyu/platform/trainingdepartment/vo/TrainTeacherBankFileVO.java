package com.guoyu.platform.trainingdepartment.vo;

public record TrainTeacherBankFileVO(
    Long id,
    String fileType,
    Long fileId,
    String legacyUrl,
    String certNo,
    Integer sortNo,
    String remark) {}
