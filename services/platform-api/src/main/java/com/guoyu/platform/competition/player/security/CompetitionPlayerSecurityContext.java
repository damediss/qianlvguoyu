package com.guoyu.platform.competition.player.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CompetitionPlayerSecurityContext {

  private CompetitionPlayerSecurityContext() {
  }

  public static CompetitionPlayerPrincipal currentPlayer() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof CompetitionPlayerPrincipal player)) {
      throw new IllegalStateException("No authenticated competition player");
    }
    return player;
  }
}
