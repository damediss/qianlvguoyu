package com.guoyu.platform.trainingdepartment.vo;

import java.util.Map;

public record TrainThreeYearPlanAnalysisStatusVO(
    Long planId,
    Long rencaiTaskId,
    String analysisStatus,
    Integer status,
    String rencaiTaskStatus,
    Integer totalRows,
    Integer processedRows,
    Double progressPct,
    Integer recommendedRows,
    Integer notRecommendedRows,
    Integer pendingManualRows,
    String errorMessage,
    Map<String, Object> raw
) {}
