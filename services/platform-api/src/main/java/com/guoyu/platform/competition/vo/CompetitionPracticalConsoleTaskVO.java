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
    String scoreSheetStatus,
    BigDecimal totalScore,
    String contestantNo,
    String contestantName,
    String refereeName,
    String assessmentSheetTitle,
    List<CompetitionPracticalConsoleQuestionVO> questions) { }
