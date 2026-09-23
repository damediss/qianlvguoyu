package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;
import java.util.List;

public record CompetitionPracticalJudgeTaskVO(
    String assignmentId,
    String sessionId,
    String eventId,
    String eventName,
    String stageId,
    String stageName,
    String workTypeName,
    String levelName,
    String contestantMemberId,
    String contestantNo,
    String contestantName,
    String refereeMemberId,
    String workstationCode,
    String status,
    String assessmentSheetId,
    CompetitionPracticalAssessmentSheetVO assessmentSheet,
    String scoreSheetId,
    String scoreSheetStatus,
    BigDecimal totalScore,
    BigDecimal scoreDifferenceLimit,
    BigDecimal actualScoreDifference,
    String scoreDifferenceStatus,
    String judgeRole,
    String startStatus,
    int confirmedJudges,
    int requiredJudges,
    boolean currentJudgeConfirmed,
    List<CompetitionPracticalQuestionProgressVO> questionProgress,
    String activeStageId) { }
