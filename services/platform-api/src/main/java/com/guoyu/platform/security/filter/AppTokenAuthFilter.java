package com.guoyu.platform.security.filter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.app.entity.PlatformUserTokenEntity;
import com.guoyu.platform.app.mapper.PlatformUserTokenMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 识别 platform_user_token 中的 SHA-1 token（对齐 PHP 旧系统）。
 * 在 SaTokenBridgeFilter 之前执行，优先匹配 App 端 token。
 */
@Slf4j
public class AppTokenAuthFilter extends OncePerRequestFilter {

  private static final String PLAYER_API_PREFIX = "/api/competition/player/";

  private final PlatformUserTokenMapper tokenMapper;
  private final PlatformUserMapper userMapper;

  public AppTokenAuthFilter(
      PlatformUserTokenMapper tokenMapper,
      PlatformUserMapper userMapper) {
    this.tokenMapper = tokenMapper;
    this.userMapper = userMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return request.getRequestURI().startsWith(PLAYER_API_PREFIX);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      filterChain.doFilter(request, response);
      return;
    }

    String authHeader = request.getHeader("Authorization");
    String token = null;
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      token = authHeader.substring(7).trim();
    }

    if (token == null || token.isEmpty()) {
      filterChain.doFilter(request, response);
      return;
    }

    log.info("AppTokenAuthFilter: checking token for URI={}", request.getRequestURI());

    try {
      PlatformUserTokenEntity tokenEntity = tokenMapper.selectOne(
          new LambdaQueryWrapper<PlatformUserTokenEntity>()
              .eq(PlatformUserTokenEntity::getToken, token)
              .last("LIMIT 1"));

      if (tokenEntity == null) {
        log.debug("AppTokenAuthFilter: token not found in platform_user_token");
        filterChain.doFilter(request, response);
        return;
      }

      PlatformUserEntity user = userMapper.selectById(tokenEntity.getUserId());
      if (user == null || user.getDeleted() != 0) {
        log.debug("AppTokenAuthFilter: user not found or deleted");
        filterChain.doFilter(request, response);
        return;
      }

      log.info("AppTokenAuthFilter: authenticated user {}", user.getId());

      PlatformUserPrincipal principal = new PlatformUserPrincipal(
          user.getId(),
          0L,
          user.getPhone(),
          user.getRealname() != null ? user.getRealname() : user.getPhone(),
          "student",
          Collections.emptyList(),
          Collections.emptyList(),
          null
      );
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (Exception e) {
      log.warn("AppTokenAuthFilter: error during authentication: {}", e.getMessage());
    }

    filterChain.doFilter(request, response);
  }
}
