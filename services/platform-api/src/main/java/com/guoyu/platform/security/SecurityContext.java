package com.guoyu.platform.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContext {

  private SecurityContext() {
  }

  public static PlatformUserPrincipal currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof PlatformUserPrincipal principal)) {
      throw new IllegalStateException("No authenticated platform user");
    }
    return principal;
  }
}
