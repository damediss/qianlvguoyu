package com.guoyu.platform.competition.player.security;

import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import com.guoyu.platform.competition.enums.CompetitionPlayerStatus;
import com.guoyu.platform.competition.mapper.CompetitionPlayerIdentityMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Accepts only the Sa-Token namespace dedicated to competition players. Platform and legacy App
 * tokens are intentionally excluded from {@code /competition/player/**}.
 */
@Component
@Slf4j
public class CompetitionPlayerTokenBridgeFilter extends OncePerRequestFilter {

  private static final String PLAYER_API_PREFIX = "/api/competition/player/";

  private final StpLogic competitionPlayerStpLogic;
  private final CompetitionPlayerIdentityMapper playerMapper;

  public CompetitionPlayerTokenBridgeFilter(
      @Qualifier("competitionPlayerStpLogic") StpLogic competitionPlayerStpLogic,
      CompetitionPlayerIdentityMapper playerMapper) {
    this.competitionPlayerStpLogic = competitionPlayerStpLogic;
    this.playerMapper = playerMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith(PLAYER_API_PREFIX);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      String token = bearerToken(request);
      if (token != null) {
        authenticate(request, token);
      }
    }
    filterChain.doFilter(request, response);
  }

  private void authenticate(HttpServletRequest request, String token) {
    try {
      Object loginId = competitionPlayerStpLogic.getLoginIdByToken(token);
      if (loginId == null || TenantContextHolder.getTenantId() == null) {
        log.warn("Competition player authentication rejected: missing token login or tenant, uri={}",
            request.getRequestURI());
        return;
      }
      Long playerId = Long.valueOf(loginId.toString());
      CompetitionPlayerIdentityEntity player = playerMapper.selectOne(
          new LambdaQueryWrapper<CompetitionPlayerIdentityEntity>()
              .eq(CompetitionPlayerIdentityEntity::getId, playerId)
              .eq(CompetitionPlayerIdentityEntity::getStatus, CompetitionPlayerStatus.ACTIVE.getCode())
              .eq(CompetitionPlayerIdentityEntity::getDeleted, 0));
      if (player == null || !TenantContextHolder.getTenantId().equals(player.getTenantId())) {
        log.warn("Competition player authentication rejected: inactive, absent, or tenant mismatch, uri={}",
            request.getRequestURI());
        return;
      }
      CompetitionPlayerPrincipal principal = new CompetitionPlayerPrincipal(
          player.getId(), player.getTenantId());
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities()));
    } catch (Exception ex) {
      log.warn("Competition player authentication failed, uri={}", request.getRequestURI());
    }
  }

  private String bearerToken(HttpServletRequest request) {
    String authorization = request.getHeader("Authorization");
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      return null;
    }
    String token = authorization.substring(7).trim();
    return token.isEmpty() ? null : token;
  }
}
