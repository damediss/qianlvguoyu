package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

public record CompetitionPracticalAssessmentSheetVO(
    String id,
    String eventId,
    String stageId,
    String eventWorkTypeId,
    String eventWorkTypeLevelId,
    String paperSnapshotId,
    String paperCode,
    Integer versionNo,
    String sheetCode,
    String title,
    Integer questionCount,
    BigDecimal totalScore,
    String status,
    JsonNode sheet) { }
