package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;
import java.util.List;

public record CompetitionPracticalJudgeTaskVO(
    String assignmentId,
    String sessionId,
    String contestantMemberId,
    String refereeMemberId,
    String workstationCode,
    String status,
    String assessmentSheetId,
    CompetitionPracticalAssessmentSheetVO assessmentSheet,
    String scoreSheetId,
    String scoreSheetStatus,
    BigDecimal totalScore,
    String judgeRole,
    String startStatus,
    int confirmedJudges,
    int requiredJudges,
    boolean currentJudgeConfirmed,
    List<CompetitionPracticalQuestionProgressVO> questionProgress) { }
