package com.guoyu.platform.auth.captcha;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CaptchaVerifyReq(
    @NotBlank String captchaId,
    @NotNull @Min(0) Integer x
) {
}
