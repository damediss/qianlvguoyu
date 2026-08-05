package com.guoyu.platform.competition.vo;
import java.math.BigDecimal;
public record CompetitionStageSettlementItemVO(Long id, Long contestantMemberId, BigDecimal totalScore, String contestantStatus, Integer displayRank, Integer tiedFlag, String promotionStatus, String reason) { }
