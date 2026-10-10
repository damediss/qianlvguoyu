package com.guoyu.platform.ai.controller;

import com.guoyu.platform.ai.service.WeKnoraEmbedApplicationService;
import com.guoyu.platform.ai.vo.WeKnoraEmbedTokenVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.PlatformUserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/ai/weknora")
public class WeKnoraEmbedController {

  private final WeKnoraEmbedApplicationService service;

  public WeKnoraEmbedController(WeKnoraEmbedApplicationService service) {
    this.service = service;
  }

  @GetMapping("/embed-token")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<WeKnoraEmbedTokenVO> embedToken() {
    return ApiResponse.ok(service.exchangeToken(currentUser()));
  }

  private PlatformUserPrincipal currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof PlatformUserPrincipal principal)) {
      log.warn("WeKnora embed token denied: no authenticated platform user");
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "Not logged in or token expired");
    }
    return principal;
  }
}
