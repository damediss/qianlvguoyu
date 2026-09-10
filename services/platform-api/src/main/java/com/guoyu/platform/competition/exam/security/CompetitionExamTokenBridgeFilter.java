package com.guoyu.platform.competition.exam.security;

import cn.dev33.satoken.stp.StpLogic;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionFormalExamAccessSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import com.guoyu.platform.competition.enums.CompetitionPlayerStatus;
import com.guoyu.platform.competition.mapper.CompetitionPlayerIdentityMapper;
import com.guoyu.platform.competition.exam.formal.CompetitionFormalExamAccessTokens;
import com.guoyu.platform.competition.service.CompetitionFormalExamAccessSessionService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class CompetitionExamTokenBridgeFilter extends OncePerRequestFilter {
  private static final List<String> PREFIXES = List.of(
      "/api/competition/exam/",
      "/api/competition/formal-exam/player/");
  private static final Set<String> LOGIN_PATHS = Set.of(
      "/api/competition/exam/auth/login",
      "/api/competition/formal-exam/player/auth/login");
  private static final String FORMAL_PLAYER_PREFIX = "/api/competition/formal-exam/player/";

  private final StpLogic examStpLogic;
  private final CompetitionPlayerIdentityMapper playerMapper;
  private final CompetitionFormalExamAccessSessionService accessSessionService;
  private final Cache<String, CachedPlayer> playerCache;
  private final Cache<String, CachedAccess> accessCache;

  public CompetitionExamTokenBridgeFilter(
      @Qualifier("competitionExamStpLogic") StpLogic examStpLogic,
      CompetitionPlayerIdentityMapper playerMapper) {
    this(examStpLogic, playerMapper, null);
  }

  @Autowired
  public CompetitionExamTokenBridgeFilter(
      @Qualifier("competitionExamStpLogic") StpLogic examStpLogic,
      CompetitionPlayerIdentityMapper playerMapper,
      CompetitionFormalExamAccessSessionService accessSessionService) {
    this.examStpLogic = examStpLogic;
    this.playerMapper = playerMapper;
    this.accessSessionService = accessSessionService;
    this.playerCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(3))
        .maximumSize(20_000)
        .build();
    this.accessCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(3))
        .maximumSize(20_000)
        .build();
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return PREFIXES.stream().noneMatch(uri::startsWith);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (LOGIN_PATHS.contains(request.getRequestURI())) {
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
      CachedPlayer cached = playerCache.getIfPresent(cacheKey(tenantId, playerId));
      if (cached == null) {
        CompetitionPlayerIdentityEntity player = playerMapper.selectById(playerId);
        if (player == null || !tenantId.equals(player.getTenantId())
            || !CompetitionPlayerStatus.allowsExamAccess(player.getStatus())) {
          log.warn("Competition exam authentication rejected: player or tenant mismatch, uri={}",
              request.getRequestURI());
          return false;
        }
        cached = new CachedPlayer(player.getTenantId(), true);
        playerCache.put(cacheKey(tenantId, playerId), cached);
      } else if (!tenantId.equals(cached.tenantId()) || !cached.allowsAccess()) {
        log.warn("Competition exam authentication rejected: cached player or tenant mismatch, uri={}",
            request.getRequestURI());
        return false;
      }
      CompetitionExamPrincipal principal;
      if (isFormalExamPlayerPath(request.getRequestURI())) {
        CachedAccess access = requireActiveAccess(token, tenantId, playerId);
        if (access == null) {
          log.warn("Formal exam authentication rejected: access session missing or inactive, uri={}",
              request.getRequestURI());
          return false;
        }
        principal = new CompetitionExamPrincipal(
            playerId, tenantId, access.accessSessionId(), access.generation(),
            access.examSessionId(), access.candidateExamId());
      } else {
        principal = new CompetitionExamPrincipal(playerId, tenantId);
      }
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities()));
      return true;
    } catch (Exception ex) {
      log.warn("Competition exam authentication failed, uri={}", request.getRequestURI());
      return false;
    }
  }

  private CachedAccess requireActiveAccess(String token, Long tenantId, Long playerId) {
    if (accessSessionService == null) {
      return null;
    }
    String fingerprintKey = CompetitionFormalExamAccessTokens.fingerprint(token);
    CachedAccess cached = accessCache.getIfPresent(fingerprintKey);
    if (cached != null) {
      if (!tenantId.equals(cached.tenantId()) || !playerId.equals(cached.playerId()) || !cached.active()) {
        return null;
      }
      return cached;
    }
    CompetitionFormalExamAccessSessionEntity access =
        accessSessionService.requireActiveByToken(token, tenantId, playerId);
    if (access == null) {
      return null;
    }
    cached = new CachedAccess(
        access.getTenantId(), access.getPlayerId(), access.getId(), access.getGeneration(),
        access.getExamSessionId(), access.getCandidateExamId(), true);
    accessCache.put(fingerprintKey, cached);
    return cached;
  }

  private boolean isFormalExamPlayerPath(String uri) {
    return uri != null && uri.startsWith(FORMAL_PLAYER_PREFIX);
  }

  private String bearerToken(HttpServletRequest request) {
    String authorization = request.getHeader("Authorization");
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      return null;
    }
    String token = authorization.substring(7).trim();
    return token.isEmpty() ? null : token;
  }

  private static String cacheKey(Long tenantId, Long playerId) {
    return tenantId + ":" + playerId;
  }

  private record CachedPlayer(Long tenantId, boolean allowsAccess) {
  }

  private record CachedAccess(
      Long tenantId,
      Long playerId,
      Long accessSessionId,
      Integer generation,
      Long examSessionId,
      Long candidateExamId,
      boolean active) {
  }
}
