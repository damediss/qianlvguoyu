package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompetitionFormalCandidateExamVO(
    String id,
    String examSessionId,
    String candidateId,
    String contestantProfileId,
    String candidateNo,
    String candidateName,
    String paperSnapshotId,
    String sourcePaperId,
    String paperCode,
    String paperName,
    String status,
    Integer answeredCount,
    Integer totalQuestionCount,
    LocalDateTime enterTime,
    LocalDateTime startTime,
    LocalDateTime submitTime,
    String submitType,
    LocalDateTime lastActiveTime,
    String forceSubmitReason,
    String forceSubmittedBy,
    LocalDateTime absentTime,
    String absentReason,
    String resultStatus,
    BigDecimal totalScore,
    BigDecimal fullScore,
    String accessSessionStatus,
    String connectionState,
    String lastSeenAt) {
}
