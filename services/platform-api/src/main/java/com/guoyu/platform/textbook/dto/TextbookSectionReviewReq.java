package com.guoyu.platform.textbook.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** W5 审定请求：action=approve/return/reopen；return/reopen 意见必填（1~1000 字）。 */
public record TextbookSectionReviewReq(
    @NotBlank @Pattern(regexp = "approve|return|reopen",
        message = "action 仅支持 approve/return/reopen") String action,
    @Size(max = 1000) String comment,
    @NotNull @Min(0) Integer baseVersion) {
}
