package com.guoyu.platform.auth.captcha;

public record CaptchaGenerateVO(
    String captchaId,
    String bgImage,
    String sliderImage,
    int sliderY
) {
}
