package com.guoyu.platform.app.controller;

import com.guoyu.platform.app.api.LegacyAppResponse;
import com.guoyu.platform.app.sms.AppSmsException;
import com.guoyu.platform.app.user.AppUserApplicationService;
import java.util.Map;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 对齐 PHP AppuserController */
@RestController
@RequestMapping("/appuser")
public class AppUserController {

  private final AppUserApplicationService userService;

  public AppUserController(AppUserApplicationService userService) {
    this.userService = userService;
  }

  @PostMapping("/infobyidcard")
  public LegacyAppResponse infoByIdCard(@RequestBody Map<String, Object> body) {
    String idcard = body != null && body.get("idcard") != null
        ? String.valueOf(body.get("idcard")).trim()
        : "";
    Map<String, Object> info = userService.infoByIdCard(idcard);
    return LegacyAppResponse.ok("成功", Map.of("info", info));
  }

  @ExceptionHandler(AppSmsException.class)
  public LegacyAppResponse handle(AppSmsException ex) {
    return LegacyAppResponse.fail(ex.getCode(), ex.getMessage());
  }
}
