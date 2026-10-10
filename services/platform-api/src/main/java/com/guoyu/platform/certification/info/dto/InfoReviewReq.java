package com.guoyu.platform.certification.info.dto;

import jakarta.validation.constraints.NotBlank;

public record InfoReviewReq(@NotBlank String reviewAction, String comment) {
}
