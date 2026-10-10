package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 批量清退正式比赛选手考试账号请求。 */
public record CompetitionFormalExamForceOfflineBatchReq(
    @NotEmpty(message = "请至少选择一名选手")
    @Size(max = 100, message = "一次最多清退100名选手")
    List<@NotNull(message = "选手考试实例不能为空") @Positive(message = "选手考试实例无效") Long> candidateExamIds) {
}
