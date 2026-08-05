package com.guoyu.platform.competition.exam.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CompetitionExamSecurityContext {
  private CompetitionExamSecurityContext() { }

  public static CompetitionExamPrincipal currentPlayer() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof CompetitionExamPrincipal principal)) {
      throw new IllegalStateException("No authenticated competition exam player");
    }
    return principal;
  }
}
