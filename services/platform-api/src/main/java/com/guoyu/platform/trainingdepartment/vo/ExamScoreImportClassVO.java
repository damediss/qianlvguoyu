package com.guoyu.platform.trainingdepartment.vo;

public record ExamScoreImportClassVO(
    Long id,
    String title,
    Long companyId,
    String companyName,
    Long trainInfoId,
    String trainTitle,
    Long workTypeId,
    String workTypeTitle,
    Integer workTypeLevel) {}
