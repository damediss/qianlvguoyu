package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompetitionMakeExamPlanVO(
    String id,
    String eventId,
    String eventWorkTypeId,
    String eventWorkTypeLevelId,
    String contentType,
    String title,
    String examType,
    Integer durationMinutes,
    BigDecimal totalScore,
    BigDecimal passScore,
    Integer paperCount,
    Integer planVersion,
    String status,
    String comments,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<CompetitionMakeExamPlanDetailVO> details
) {
}
