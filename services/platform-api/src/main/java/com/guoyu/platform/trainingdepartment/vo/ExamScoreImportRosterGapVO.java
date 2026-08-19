package com.guoyu.platform.trainingdepartment.vo;

public record ExamScoreImportRosterGapVO(
    Long peopleId,
    Long classId,
    String classTitle,
    String rosterName,
    String idCard) {}
