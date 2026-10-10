package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.NotNull;

/** FAQ 转换记录查询请求 */
public record FaqConversionQueryReq(
    @NotNull Long batchId,
    String conversionStatus,
    String tagName,
    String keyword,
    Long page,
    Long pageSize
) {
}
