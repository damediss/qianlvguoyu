package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompetitionPracticalRuleVO(
    Long id, Long eventId, Long stageId, Long eventWorkTypeId, Long eventWorkTypeLevelId,
    Integer ruleVersion, String ruleName, BigDecimal totalScore, String status,
    LocalDateTime publishedAt, List<CompetitionPracticalRuleItemVO> items) { }
