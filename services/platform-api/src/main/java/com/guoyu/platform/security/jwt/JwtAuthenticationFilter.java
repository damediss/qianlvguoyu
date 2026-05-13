package com.guoyu.platform.security.jwt;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.RoleMenuMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.system.mapper.UserRoleMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenService jwtTokenService;
  private final UserMapper userMapper;
  private final UserRoleMapper userRoleMapper;
  private final RoleMenuMapper roleMenuMapper;

  public JwtAuthenticationFilter(
      JwtTokenService jwtTokenService,
      UserMapper userMapper,
      UserRoleMapper userRoleMapper,
      RoleMenuMapper roleMenuMapper
  ) {
    this.jwtTokenService = jwtTokenService;
    this.userMapper = userMapper;
    this.userRoleMapper = userRoleMapper;
    this.roleMenuMapper = roleMenuMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String token = resolveBearerToken(request);
    if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      try {
        authenticate(token);
      } catch (JwtException | IllegalArgumentException ex) {
        SecurityContextHolder.clearContext();
      }
    }
    filterChain.doFilter(request, response);
  }

  private void authenticate(String token) {
    Claims claims = jwtTokenService.parse(token);
    Long userId = Long.valueOf(claims.getSubject());
    Long tenantId = ((Number) claims.get("tenantId")).longValue();
    UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getId, userId)
        .eq(UserEntity::getTenantId, tenantId)
        .eq(UserEntity::getStatus, "enabled")
        .eq(UserEntity::getDeleted, 0));
    if (user == null) {
      return;
    }

    List<String> roleCodes = userRoleMapper.selectRoleCodes(tenantId, userId);
    List<String> permissionCodes = roleMenuMapper.selectPermissionCodesByUser(tenantId, userId);
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        user.getId(),
        user.getTenantId(),
        user.getUsername(),
        user.getDisplayName(),
        user.getUserType(),
        roleCodes,
        permissionCodes
    );
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private String resolveBearerToken(HttpServletRequest request) {
    String authorization = request.getHeader("Authorization");
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      return null;
    }
    return authorization.substring(7);
  }
}
