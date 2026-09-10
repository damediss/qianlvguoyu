package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CompetitionPracticalScoreEntryReq(
    /** Legacy stage-rule item id; nullable when the bound competition paper is used. */
    Long ruleItemId,
    /** Immutable competition paper-question id used by the new snapshot scoring path. */
    Long paperQuestionId,
    /** Immutable scoring-item number inside the paper-question snapshot. */
    Integer paperItemNo,
    @NotNull @DecimalMin("0.00") BigDecimal score,
    @NotNull @DecimalMin("0.00") BigDecimal deduction,
    String remark) { }
