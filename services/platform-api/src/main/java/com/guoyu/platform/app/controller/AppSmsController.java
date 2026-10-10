package com.guoyu.platform.app.controller;

import com.guoyu.platform.app.api.LegacyAppResponse;
import com.guoyu.platform.app.sms.AppSmsApplicationService;
import com.guoyu.platform.app.sms.AppSmsException;
import java.util.Map;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 对齐 PHP AppsmsController */
@RestController
@RequestMapping("/appsms")
public class AppSmsController {

  private final AppSmsApplicationService smsService;

  public AppSmsController(AppSmsApplicationService smsService) {
    this.smsService = smsService;
  }

  @PostMapping("/send")
  public LegacyAppResponse send(@RequestBody Map<String, Object> body) {
    String phone = body != null && body.get("phone") != null ? String.valueOf(body.get("phone")) : "";
    smsService.sendCode(phone);
    return LegacyAppResponse.ok("成功", null);
  }

  @ExceptionHandler(AppSmsException.class)
  public LegacyAppResponse handle(AppSmsException ex) {
    return LegacyAppResponse.fail(ex.getCode(), ex.getMessage());
  }
}
