package com.guoyu.platform.competition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CompetitionFormalCandidateAnswerSheetExportReq(
    @NotEmpty(message = "请选择要导出的选手")
    @Size(max = 200, message = "一次最多导出 200 份答题卡")
    @Valid
    List<@NotNull(message = "选手考试实例不能为空") Long> candidateExamIds) {
}
