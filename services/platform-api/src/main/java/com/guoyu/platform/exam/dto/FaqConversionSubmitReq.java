package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 发起 FAQ 转换请求 */
public record FaqConversionSubmitReq(
    @NotNull Integer workTypeId,
    List<Integer> courseIds,
    List<String> questionTypes,
    @NotNull String modelCode,
    Integer concurrencyLimit
) {
}
