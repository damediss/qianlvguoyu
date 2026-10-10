package com.guoyu.platform.auth.captcha;

public record CaptchaVerifyVO(
    boolean success,
    String captchaToken,
    String message
) {
  public static CaptchaVerifyVO ok(String token) {
    return new CaptchaVerifyVO(true, token, null);
  }

  public static CaptchaVerifyVO fail(String message) {
    return new CaptchaVerifyVO(false, null, message);
  }
}
