package com.guoyu.platform.textbook.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** W4 提交审定请求：基于当前 version 提交。 */
public record TextbookSectionSubmitReq(@NotNull @Min(0) Integer baseVersion) {
}
