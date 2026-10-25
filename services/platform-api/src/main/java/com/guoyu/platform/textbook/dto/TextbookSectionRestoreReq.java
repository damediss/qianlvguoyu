package com.guoyu.platform.textbook.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** W9 恢复历史版本请求：snapshotId 必须属于本节。 */
public record TextbookSectionRestoreReq(
    @NotBlank String snapshotId,
    @NotNull @Min(0) Integer baseVersion) {
}
