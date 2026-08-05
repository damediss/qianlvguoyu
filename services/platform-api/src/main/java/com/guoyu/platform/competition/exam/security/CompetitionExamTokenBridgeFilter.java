package com.guoyu.platform.competition.exam.security;

import cn.dev33.satoken.stp.StpLogic;
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

@Component
@Slf4j
public class CompetitionExamTokenBridgeFilter extends OncePerRequestFilter {
  private static final String PREFIX = "/api/competition/exam/";
  private static final String LOGIN_PATH = "/api/competition/exam/auth/login";

  private final StpLogic examStpLogic;
  private final CompetitionPlayerIdentityMapper playerMapper;

  public CompetitionExamTokenBridgeFilter(
      @Qualifier("competitionExamStpLogic") StpLogic examStpLogic,
      CompetitionPlayerIdentityMapper playerMapper) {
    this.examStpLogic = examStpLogic;
    this.playerMapper = playerMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith(PREFIX);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (LOGIN_PATH.equals(request.getRequestURI())) {
      filterChain.doFilter(request, response);
      return;
    }
    String token = bearerToken(request);
    if (token == null || !authenticate(request, token)) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write("{\"code\":\"401\",\"message\":\"考试会话无效或已下线\"}");
      return;
    }
    filterChain.doFilter(request, response);
  }

  private boolean authenticate(HttpServletRequest request, String token) {
    try {
      Object loginId = examStpLogic.getLoginIdByToken(token);
      Long tenantId = TenantContextHolder.getTenantId();
      if (loginId == null || tenantId == null) {
        log.warn("Competition exam authentication rejected: missing login or tenant, uri={}",
            request.getRequestURI());
        return false;
      }
      Long playerId = Long.valueOf(loginId.toString());
      CompetitionPlayerIdentityEntity player = playerMapper.selectById(playerId);
      if (player == null || !tenantId.equals(player.getTenantId())
          || !CompetitionPlayerStatus.ACTIVE.getCode().equals(player.getStatus())) {
        log.warn("Competition exam authentication rejected: player or tenant mismatch, uri={}",
            request.getRequestURI());
        return false;
      }
      CompetitionExamPrincipal principal = new CompetitionExamPrincipal(playerId, tenantId);
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities()));
      return true;
    } catch (Exception ex) {
      log.warn("Competition exam authentication failed, uri={}", request.getRequestURI());
      return false;
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
