package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;
import java.util.List;

public record CompetitionPracticalConsoleTaskVO(
    String assignmentId,
    String sessionId,
    String contestantMemberId,
    String refereeMemberId,
    String assessmentSheetId,
    String workstationCode,
    String taskStatus,
    String scoreSheetId,
    String scoreSheetStatus,
    BigDecimal totalScore,
    BigDecimal scoreDifferenceLimit,
    BigDecimal actualScoreDifference,
    String scoreDifferenceStatus,
    boolean practicalScoreCompleted,
    String contestantNo,
    String contestantName,
    String refereeName,
    String assessmentSheetTitle,
    List<CompetitionPracticalConsoleQuestionVO> questions) { }
