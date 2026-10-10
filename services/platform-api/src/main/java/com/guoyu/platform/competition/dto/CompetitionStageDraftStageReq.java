package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** One business stage in a complete draft-save request. */
public record CompetitionStageDraftStageReq(
    Long id,
    @NotBlank(message = "赛段类型不能为空") @Size(max = 32, message = "赛段类型长度不合法") String stageType,
    LocalDateTime startAt,
    LocalDateTime endAt,
    @Size(max = 2000, message = "赛段说明长度不能超过2000个字符") String description) {
}
