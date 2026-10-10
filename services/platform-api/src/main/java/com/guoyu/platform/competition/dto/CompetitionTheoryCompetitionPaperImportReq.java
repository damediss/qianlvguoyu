package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 将一个或多个独立组卷计划产出的已发布试卷绑定到某个赛段。 */
public record CompetitionTheoryCompetitionPaperImportReq(
    @NotNull Long stageId,
    @NotNull Long eventWorkTypeId,
    @NotNull Long eventWorkTypeLevelId,
    @NotEmpty @Size(max = 26) List<@NotNull Long> paperSnapshotIds
) {
}
