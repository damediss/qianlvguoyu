package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;

public record CompetitionMakeExamPlanTopicRuleVO(
    String id,
    String blueprintNodeId,
    Integer questionCount,
    BigDecimal score,
    Boolean selected,
    Integer sortOrder
) {
}
