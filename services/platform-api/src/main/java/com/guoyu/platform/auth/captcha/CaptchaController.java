package com.guoyu.platform.auth.captcha;

import com.guoyu.platform.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/captcha")
@Slf4j
public class CaptchaController {

  private final CaptchaService captchaService;

  public CaptchaController(CaptchaService captchaService) {
    this.captchaService = captchaService;
  }

  @GetMapping("/generate")
  public ApiResponse<CaptchaGenerateVO> generate() {
    return ApiResponse.ok(captchaService.generate());
  }

  @PostMapping("/verify")
  public ApiResponse<CaptchaVerifyVO> verify(@Valid @RequestBody CaptchaVerifyReq req) {
    CaptchaVerifyVO result = captchaService.verify(req.captchaId(), req.x());
    if (result.success()) {
      return ApiResponse.ok(result);
    }
    return ApiResponse.fail("400", result.message());
  }
}
