package com.guoyu.platform.exam.zhenti.dto;

import jakarta.validation.constraints.NotNull;

/** A1 App 端真题清单入参。 */
public record ZhentiAppListReq(@NotNull(message = "考试项目不能为空") Long examId) {
}
