package com.guoyu.platform.competition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Complete replacement payload for the draft stage structure. */
public record CompetitionStageDraftSaveReq(
    @NotNull(message = "赛段数组不能为空")
    @Size(min = 1, max = 5, message = "赛事必须配置1至5个赛段")
    List<@Valid CompetitionStageDraftStageReq> stages) {
}
