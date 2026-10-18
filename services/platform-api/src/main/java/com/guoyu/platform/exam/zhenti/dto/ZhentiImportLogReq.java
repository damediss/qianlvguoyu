package com.guoyu.platform.exam.zhenti.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * D6 导入记录查询：importId 与 examId 二选一。
 *
 * <p>按 importId 查询时忽略分页；按 examId 查询时返回历史导入记录分页。
 */
public record ZhentiImportLogReq(
    Long importId,
    Long examId,
    @Min(value = 1, message = "page 最小为 1") Long page,
    @JsonAlias("size")
    @Min(value = 1, message = "size 最小为 1")
    @Max(value = 100, message = "size 最大为 100")
    Long pageSize
) {
}
