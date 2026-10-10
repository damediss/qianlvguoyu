package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompetitionFormalExamForceSubmitReq(
    @NotBlank(message = "强制交卷原因不能为空")
    @Size(min = 1, max = 500, message = "强制交卷原因长度为 1～500 字")
    String reason) {
}
