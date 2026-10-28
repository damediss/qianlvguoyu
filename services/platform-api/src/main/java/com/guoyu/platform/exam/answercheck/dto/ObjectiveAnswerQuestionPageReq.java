package com.guoyu.platform.exam.answercheck.dto;

import jakarta.validation.Valid;

/** 试题分页请求（方案 §5.3）：服务端分页，只允许 20/50/100 条每页。 */
public record ObjectiveAnswerQuestionPageReq(
    @Valid ObjectiveAnswerScope scope,
    Long page,
    Long pageSize) {
}
