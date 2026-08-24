package com.guoyu.platform.security.filter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.app.entity.PlatformUserTokenEntity;
import com.guoyu.platform.app.mapper.PlatformUserTokenMapper;
import com.guoyu.platform.integration.php.PhpTokenQueryService;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 识别 platform_user_token 中的 SHA-1 token（对齐 PHP 旧系统）。
 * 在 SaTokenBridgeFilter 之前执行，优先匹配 App 端 token。
 *
 * <p>镜像 miss 兜底：token 同步为快周期任务（默认 3s），PHP 单点登录 re-login 后
 * 新 token 在窗口内不在镜像中，会假性 401 把学员踢到登录页。此时直查 PHP 源库
 * （只读）并回种镜像，鉴权结果与 PHP 实时一致。
 */
@Slf4j
public class AppTokenAuthFilter extends OncePerRequestFilter {

  private static final String PLAYER_API_PREFIX = "/api/competition/player/";

  private final PlatformUserTokenMapper tokenMapper;
  private final PlatformUserMapper userMapper;
  private final PhpTokenQueryService phpTokenQueryService;

  public AppTokenAuthFilter(
      PlatformUserTokenMapper tokenMapper,
      PlatformUserMapper userMapper,
      PhpTokenQueryService phpTokenQueryService) {
    this.tokenMapper = tokenMapper;
    this.userMapper = userMapper;
    this.phpTokenQueryService = phpTokenQueryService;
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
    Authentication existing = SecurityContextHolder.getContext().getAuthentication();
    // 已有 App 主体则跳过；Anonymous 不能挡住 Bearer itoken 验票
    if (existing != null
        && existing.isAuthenticated()
        && !(existing instanceof AnonymousAuthenticationToken)
        && existing.getPrincipal() instanceof PlatformUserPrincipal) {
      filterChain.doFilter(request, response);
      return;
    }
    if (existing instanceof AnonymousAuthenticationToken) {
      SecurityContextHolder.clearContext();
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
      // token 在 PHP 源库允许重复（同秒同密码 sha1 相同，现网实测 85 组），
      // 按 id 升序取首行，与 PHP Phalcon findFirst 语义一致，保证两端鉴权结果相同
      PlatformUserTokenEntity tokenEntity = tokenMapper.selectOne(
          new LambdaQueryWrapper<PlatformUserTokenEntity>()
              .eq(PlatformUserTokenEntity::getToken, token)
              .orderByAsc(PlatformUserTokenEntity::getId)
              .last("LIMIT 1"));

      Long userId;
      if (tokenEntity != null) {
        userId = tokenEntity.getUserId();
      } else {
        // 镜像 miss：直查 PHP 源库兜底（消除快周期同步窗口的假性 401），命中已回种镜像
        userId = phpTokenQueryService.findUserIdByToken(token);
        if (userId == null) {
          log.debug("AppTokenAuthFilter: token not found in mirror or PHP source");
          filterChain.doFilter(request, response);
          return;
        }
      }

      PlatformUserEntity user = userMapper.selectById(userId);
      if (user == null || user.getDeleted() != 0) {
        log.debug("AppTokenAuthFilter: user not found or deleted");
        filterChain.doFilter(request, response);
        return;
      }

      log.info("AppTokenAuthFilter: authenticated user {}", user.getId());

      Long tenantId = com.guoyu.platform.common.tenant.TenantContextHolder.DEFAULT_TENANT_ID;
      String tenantHeader = request.getHeader("X-Tenant-Id");
      if (tenantHeader != null && !tenantHeader.isBlank()) {
        try {
          tenantId = Long.parseLong(tenantHeader.trim());
        } catch (NumberFormatException ignored) {
          // keep default SaaS tenant
        }
      }

      PlatformUserPrincipal principal = new PlatformUserPrincipal(
          user.getId(),
          tenantId,
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
