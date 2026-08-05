package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompetitionPracticalScoreSheetVO(
    Long id, Long assignmentId, Long ruleId, Long contestantMemberId, Long refereeMemberId,
    Integer revisionNo, String status, BigDecimal totalScore, LocalDateTime openedAt,
    LocalDateTime submittedAt, LocalDateTime lockedAt, List<CompetitionPracticalScoreEntryVO> entries) { }
