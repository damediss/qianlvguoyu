package com.guoyu.platform.exam.zhenti.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** D1 管理端列表查询。 */
public record ZhentiQueryReq(
    @NotNull(message = "考试项目不能为空") Long examId,
    @Min(value = 1, message = "page 最小为 1") long page,
    @JsonAlias("size")
    @Min(value = 1, message = "size 最小为 1")
    @Max(value = 100, message = "size 最大为 100")
    long pageSize,
    String keyword,
    String year,
    Integer status,
    Integer metaPending
) {
}
