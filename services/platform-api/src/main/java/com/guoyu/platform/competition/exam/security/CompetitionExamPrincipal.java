package com.guoyu.platform.competition.exam.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.guoyu.platform.security.TenantScopedPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

public class CompetitionExamPrincipal implements UserDetails, TenantScopedPrincipal {
  private final Long playerId;
  private final Long tenantId;
  private final Long accessSessionId;
  private final Integer generation;
  private final Long examSessionId;
  private final Long candidateExamId;
  private final Collection<? extends GrantedAuthority> authorities;

  public CompetitionExamPrincipal(Long playerId, Long tenantId) {
    this(playerId, tenantId, null, null, null, null);
  }

  public CompetitionExamPrincipal(
      Long playerId,
      Long tenantId,
      Long accessSessionId,
      Integer generation,
      Long examSessionId,
      Long candidateExamId) {
    this.playerId = playerId;
    this.tenantId = tenantId;
    this.accessSessionId = accessSessionId;
    this.generation = generation;
    this.examSessionId = examSessionId;
    this.candidateExamId = candidateExamId;
    this.authorities = List.of(new SimpleGrantedAuthority("ROLE_COMPETITION_EXAM"));
  }

  public Long getPlayerId() { return playerId; }
  @Override
  public Long getTenantId() { return tenantId; }
  public Long getAccessSessionId() { return accessSessionId; }
  public Integer getGeneration() { return generation; }
  public Long getExamSessionId() { return examSessionId; }
  public Long getCandidateExamId() { return candidateExamId; }
  @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
  @Override public String getPassword() { return ""; }
  @Override public String getUsername() { return String.valueOf(playerId); }
  @Override public boolean isAccountNonExpired() { return true; }
  @Override public boolean isAccountNonLocked() { return true; }
  @Override public boolean isCredentialsNonExpired() { return true; }
  @Override public boolean isEnabled() { return true; }
}
