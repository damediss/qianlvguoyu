package com.guoyu.platform.app.controller;

import com.guoyu.platform.app.api.LegacyAppResponse;
import com.guoyu.platform.app.auth.AppAuthApplicationService;
import com.guoyu.platform.app.sms.AppSmsException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.util.StringUtils;
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

  //仅凭身份证+手机号、不验密码就签发 token 写入 platform_user_token,感觉风险较大先注释
//  @PostMapping("/setlogin")
  public LegacyAppResponse setLogin(@RequestBody Map<String, Object> body) {
    String idcard = str(body, "idcard");
    String phone = str(body, "phone");
    String isAuth = str(body, "isAuth");
    return LegacyAppResponse.ok("成功", authService.setLogin(idcard, phone, isAuth));
  }

  /** 退出登录：服务端删除当前 token，前端再清本地存储 */
  @PostMapping("/logout")
  public LegacyAppResponse logout(HttpServletRequest request) {
    String token = extractBearerToken(request);
    authService.logout(token);
    return LegacyAppResponse.ok("退出成功", null);
  }

  private static String extractBearerToken(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7).trim();
    }
    // 兼容前端直接传 itoken 头的情况
    String itoken = request.getHeader("itoken");
    return StringUtils.hasText(itoken) ? itoken.trim() : null;
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
