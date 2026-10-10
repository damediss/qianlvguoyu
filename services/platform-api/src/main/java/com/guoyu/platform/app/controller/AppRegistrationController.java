package com.guoyu.platform.app.controller;

import com.guoyu.platform.app.api.LegacyAppResponse;
import com.guoyu.platform.app.registration.AppRegistrationApplicationService;
import com.guoyu.platform.app.registration.AppRegistrationException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 小程序自主报名（对齐 PHP AppregistrationController） */
@RestController
@RequestMapping("/appregistration")
@Slf4j
public class AppRegistrationController {

  private final AppRegistrationApplicationService appRegistrationApplicationService;

  public AppRegistrationController(AppRegistrationApplicationService appRegistrationApplicationService) {
    this.appRegistrationApplicationService = appRegistrationApplicationService;
  }

  @PostMapping("/selflist")
  public LegacyAppResponse selfList(@RequestBody(required = false) Map<String, Object> body) {
    int page = 1;
    if (body != null && body.get("page") != null) {
      try {
        page = Integer.parseInt(String.valueOf(body.get("page")));
      } catch (NumberFormatException ignored) {
        page = 1;
      }
    }
    return LegacyAppResponse.ok("成功", appRegistrationApplicationService.selfList(page));
  }

  @PostMapping("/selfinfo")
  public LegacyAppResponse selfInfo(
      @RequestBody Map<String, Object> body,
      @RequestParam(value = "istate", required = false) String istate) {
    Long id = longVal(body != null ? body.get("id") : null);
    return LegacyAppResponse.ok("成功", appRegistrationApplicationService.selfInfo(id, istate));
  }

  @PostMapping("/submit")
  public LegacyAppResponse submit(
      @RequestBody Map<String, Object> body,
      @RequestParam(value = "istate", required = false) String istate) {
    Long signUpId = longVal(body != null ? body.get("id") : null);
    @SuppressWarnings("unchecked")
    Map<String, Object> formData = body != null && body.get("formData") instanceof Map<?, ?> map
        ? (Map<String, Object>) map
        : Map.of();
    Map<String, Object> result = appRegistrationApplicationService.submit(signUpId, formData, istate);
    String msg = String.valueOf(result.get("msg"));
    result.remove("msg");
    return LegacyAppResponse.ok(msg, result);
  }

  @ExceptionHandler(AppRegistrationException.class)
  public LegacyAppResponse handleAppRegistrationException(AppRegistrationException ex) {
    return LegacyAppResponse.fail(ex.getCode(), ex.getMessage(), ex.getData());
  }

  private static Long longVal(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(value).trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
