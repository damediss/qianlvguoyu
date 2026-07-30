package com.guoyu.platform.security.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.security.PlatformAdminSupport;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.RoleMenuMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.system.mapper.UserRoleMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class SaTokenBridgeFilter extends OncePerRequestFilter {

  private final UserMapper userMapper;
  private final UserRoleMapper userRoleMapper;
  private final RoleMenuMapper roleMenuMapper;

  public SaTokenBridgeFilter(
      UserMapper userMapper,
      UserRoleMapper userRoleMapper,
      RoleMenuMapper roleMenuMapper
  ) {
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
    String token = StpUtil.getTokenValue();
    if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      try {
        authenticate(token);
      } catch (Exception ex) {
        log.warn("Sa-Token authentication failed for request {}: {}", request.getRequestURI(), ex.getMessage());
        SecurityContextHolder.clearContext();
      }
    }
    filterChain.doFilter(request, response);
  }

  private void authenticate(String token) {
    Object loginIdObj = StpUtil.getLoginIdByToken(token);
    if (loginIdObj == null) {
      return;
    }
    Long userId = Long.valueOf(loginIdObj.toString());

    UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getId, userId)
        .eq(UserEntity::getStatus, "enabled")
        .eq(UserEntity::getDeleted, 0));
    if (user == null) {
      return;
    }

    // tenantId comes directly from the user record (tik tokens don't support extras)
    Long tenantId = user.getTenantId();

    List<String> roleCodes = userRoleMapper.selectRoleCodes(tenantId, userId);
    if (roleCodes == null) {
      roleCodes = List.of();
    }
    List<String> permissionCodes = PlatformAdminSupport.isSuperAdmin(roleCodes, user.getUsername())
        ? roleMenuMapper.selectAllPermissionCodes(tenantId)
        : roleMenuMapper.selectPermissionCodesByUser(tenantId, userId);
    if (permissionCodes == null) {
      permissionCodes = List.of();
    }
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
}
