package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;
import java.util.List;

public record ExamScoreImportBatchVO(
    Long id,
    String fileName,
    String planName,
    String planCode,
    String examTimeText,
    String layout,
    List<Long> classIds,
    int totalRows,
    int matchedCount,
    int writtenCount,
    int overwrittenCount,
    int skippedCount,
    int unmatchedCount,
    String operatorUsername,
    LocalDateTime createdAt) {}
