package com.guoyu.platform.trainingdepartment.vo;

import java.util.List;

public record ExamScoreImportPreviewVO(
    String previewId,
    String planName,
    String planCode,
    String examTimeText,
    String layout,
    List<Long> classIds,
    int totalRows,
    int matched,
    int nameWarn,
    int jobWarn,
    int unmatched,
    int emptyScore,
    int sheetDup,
    int classMissingOnSheet,
    int classMissingIdCard,
    List<ExamScoreImportItemVO> items,
    List<ExamScoreImportRosterGapVO> classMissingPeople,
    List<ExamScoreImportRosterGapVO> missingIdCardPeople) {}
