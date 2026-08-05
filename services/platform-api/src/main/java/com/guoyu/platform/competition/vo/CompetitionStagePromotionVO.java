package com.guoyu.platform.competition.vo;
import java.time.LocalDateTime;
import java.util.List;
public record CompetitionStagePromotionVO(Long id, Long settlementId, Long sourcePlanId, Long sourceStageId, Long targetStageId, Integer quota, String promotionRuleType, Integer promotionPercentage, Integer calculatedTargetCount, String referenceStatus, String promotionStatus, String finalPodiumStatus, Integer promotionVersion, String status, LocalDateTime publishedAt, List<CompetitionStagePromotionItemVO> items) { }
