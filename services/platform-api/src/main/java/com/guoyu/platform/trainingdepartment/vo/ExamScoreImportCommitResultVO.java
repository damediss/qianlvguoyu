package com.guoyu.platform.trainingdepartment.vo;

public record ExamScoreImportCommitResultVO(
    Long batchId,
    int written,
    int overwritten,
    int skipped,
    int unmatched) {}
