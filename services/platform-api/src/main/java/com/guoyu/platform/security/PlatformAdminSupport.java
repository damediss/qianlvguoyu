package com.guoyu.platform.security;

import java.util.List;

public final class PlatformAdminSupport {

  public static final String ROLE_PLATFORM_ADMIN = "PLATFORM_ADMIN";
  public static final String USERNAME_SUPER_ADMIN = "admin";

  private PlatformAdminSupport() {
  }

  public static boolean isPlatformAdmin(List<String> roleCodes) {
    return roleCodes != null && roleCodes.stream().anyMatch(ROLE_PLATFORM_ADMIN::equalsIgnoreCase);
  }

  public static boolean isSuperAdmin(List<String> roleCodes, String username) {
    return isPlatformAdmin(roleCodes)
        || (username != null && USERNAME_SUPER_ADMIN.equalsIgnoreCase(username));
  }

  public static boolean isSuperAdmin(PlatformUserPrincipal principal) {
    return principal != null && isSuperAdmin(principal.getRoleCodes(), principal.getUsername());
  }
}
