package com.guoyu.platform.textbook.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** W3 自动/手动保存请求（方案 §6.3）。baseVersion=0 表示首存。 */
public record TextbookSectionSaveReq(
    @NotNull @Size(max = 1_000_000) String contentMd,
    @NotNull @Min(0) Integer baseVersion) {
}
