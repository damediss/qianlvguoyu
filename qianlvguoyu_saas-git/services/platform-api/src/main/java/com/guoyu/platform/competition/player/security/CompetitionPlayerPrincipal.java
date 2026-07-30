package com.guoyu.platform.competition.player.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Security principal for the dedicated competition-player token namespace. */
public class CompetitionPlayerPrincipal implements UserDetails {

  private final Long playerId;
  private final Long tenantId;
  private final Collection<? extends GrantedAuthority> authorities;

  public CompetitionPlayerPrincipal(Long playerId, Long tenantId) {
    this.playerId = playerId;
    this.tenantId = tenantId;
    this.authorities = List.of(new SimpleGrantedAuthority("ROLE_COMPETITION_PLAYER"));
  }

  public Long getPlayerId() {
    return playerId;
  }

  public Long getTenantId() {
    return tenantId;
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
    return String.valueOf(playerId);
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
