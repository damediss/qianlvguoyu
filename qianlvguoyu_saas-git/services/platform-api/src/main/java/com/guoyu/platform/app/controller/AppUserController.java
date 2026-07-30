package com.guoyu.platform.app.controller;

import com.guoyu.platform.app.api.LegacyAppResponse;
import com.guoyu.platform.app.sms.AppSmsException;
import com.guoyu.platform.app.user.AppUserApplicationService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 对齐 PHP AppuserController（v4 为兼容旧前端路径） */
@RestController
@RequestMapping({"/appuser", "/appuserv4"})
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

  @PostMapping("/info")
  public LegacyAppResponse info(@RequestBody(required = false) Map<String, Object> body) {
    Long userId = currentUserIdOrBody(body);
    return LegacyAppResponse.ok("成功", userService.userInfo(userId));
  }

  @PostMapping("/index")
  public LegacyAppResponse index(@RequestBody(required = false) Map<String, Object> body) {
    Long userId = currentUserIdOrBody(body);
    return LegacyAppResponse.ok("成功", userService.myPageData(userId));
  }

  @PostMapping("/detail")
  public LegacyAppResponse detail(@RequestBody(required = false) Map<String, Object> body) {
    Long userId = currentUserIdOrBody(body);
    return LegacyAppResponse.ok("成功", userService.detail(userId));
  }

  @PostMapping("/detailsave")
  public LegacyAppResponse detailSave(@RequestBody(required = false) Map<String, Object> body) {
    Long userId = currentUserIdOrBody(body);
    @SuppressWarnings("unchecked")
    Map<String, Object> form = body != null && body.get("form") instanceof Map<?, ?> m
        ? (Map<String, Object>) m : Map.of();
    return LegacyAppResponse.ok("保存成功", userService.detailSave(userId, form));
  }

  private Long currentUserIdOrBody(Map<String, Object> body) {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null
          && authentication.getPrincipal() instanceof PlatformUserPrincipal principal
          && principal.getUserId() != null) {
        return principal.getUserId();
      }
    } catch (Exception ignore) {
      // 未登录时降级使用 body
    }
    if (body != null && body.get("userId") != null) {
      try {
        return Long.parseLong(String.valueOf(body.get("userId")));
      } catch (NumberFormatException ignore) {
        return null;
      }
    }
    return null;
  }

  @ExceptionHandler(AppSmsException.class)
  public LegacyAppResponse handle(AppSmsException ex) {
    return LegacyAppResponse.fail(ex.getCode(), ex.getMessage());
  }
}
