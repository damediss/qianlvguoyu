package com.guoyu.platform.app.controller;

import com.guoyu.platform.app.api.LegacyAppResponse;
import com.guoyu.platform.app.auth.AppAuthApplicationService;
import com.guoyu.platform.app.sms.AppSmsException;
import java.util.Map;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 对齐 PHP AppauthController */
@RestController
@RequestMapping("/appauth")
public class AppAuthController {

  private final AppAuthApplicationService authService;

  public AppAuthController(AppAuthApplicationService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public LegacyAppResponse login(@RequestBody Map<String, Object> body) {
    String phone = str(body, "phone");
    String password = str(body, "password");
    return LegacyAppResponse.ok("成功", authService.login(phone, password));
  }

  @PostMapping("/checkcode")
  public LegacyAppResponse checkCode(@RequestBody Map<String, Object> body) {
    String phone = str(body, "phone");
    String code = str(body, "code");
    authService.checkCode(phone, code);
    return LegacyAppResponse.ok("成功", null);
  }

  @PostMapping("/setlogin")
  public LegacyAppResponse setLogin(@RequestBody Map<String, Object> body) {
    String idcard = str(body, "idcard");
    String phone = str(body, "phone");
    String isAuth = str(body, "isAuth");
    return LegacyAppResponse.ok("成功", authService.setLogin(idcard, phone, isAuth));
  }

  @ExceptionHandler(AppSmsException.class)
  public LegacyAppResponse handle(AppSmsException ex) {
    return LegacyAppResponse.fail(ex.getCode(), ex.getMessage());
  }

  private static String str(Map<String, Object> body, String key) {
    if (body == null || body.get(key) == null) {
      return "";
    }
    return String.valueOf(body.get(key)).trim();
  }
}
