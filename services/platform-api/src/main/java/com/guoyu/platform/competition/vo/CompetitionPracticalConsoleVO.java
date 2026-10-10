package com.guoyu.platform.competition.vo;

import java.util.List;

public record CompetitionPracticalConsoleVO(
    String planId,
    String eventId,
    String stageId,
    String eventWorkTypeId,
    String eventWorkTypeLevelId,
    int sessionCount,
    int contestantCount,
    int scoringTaskCount,
    int openedSheetCount,
    int submittedSheetCount,
    String status,
    List<CompetitionPracticalConsoleTaskVO> tasks) { }
