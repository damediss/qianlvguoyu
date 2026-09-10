package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A work-type/level pair selected for one competition player.
 *
 * <p>The level id is the authoritative value.  levelCode/level are retained
 * only so older clients can be upgraded without losing their submitted text.
 */
public record CompetitionPlayerWorkTypeReq(
    @NotNull Long eventWorkTypeId,
    Long eventWorkTypeLevelId,
    @Size(max = 32) String levelCode,
    @Size(max = 32) String level) {
}
