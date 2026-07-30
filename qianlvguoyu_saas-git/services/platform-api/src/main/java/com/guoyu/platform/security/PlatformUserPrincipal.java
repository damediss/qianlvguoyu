package com.guoyu.platform.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class PlatformUserPrincipal implements UserDetails {

  private final Long userId;
  private final Long tenantId;
  private final String username;
  private final String displayName;
  private final String userType;
  private final String email;
  private final List<String> roleCodes;
  private final List<String> permissionCodes;
  private final Collection<? extends GrantedAuthority> authorities;

  public PlatformUserPrincipal(
      Long userId,
      Long tenantId,
      String username,
      String displayName,
      String userType,
      List<String> roleCodes,
      List<String> permissionCodes,
      String email
  ) {
    this.userId = userId;
    this.tenantId = tenantId;
    this.username = username;
    this.displayName = displayName;
    this.userType = userType;
    this.email = email;
    this.roleCodes = List.copyOf(roleCodes);
    this.permissionCodes = List.copyOf(permissionCodes);
    this.authorities = permissionCodes.stream()
        .map(SimpleGrantedAuthority::new)
        .toList();
  }

  public Long getUserId() {
    return userId;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getUserType() {
    return userType;
  }

  public String getEmail() {
    return email;
  }

  public List<String> getRoleCodes() {
    return roleCodes;
  }

  public List<String> getPermissionCodes() {
    return permissionCodes;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return "";
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
