package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;

public record CompetitionStagePaperAssignmentPaperVO(
    String id,
    String paperCode,
    String title,
    Integer versionNo,
    Integer assignedCount,
    BigDecimal percentage) {
}
