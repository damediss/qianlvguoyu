package com.guoyu.platform.security.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
    Long tenantId = StpUtil.getExtra(token, "tenantId") != null
        ? Long.valueOf(StpUtil.getExtra(token, "tenantId").toString())
        : null;

    UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getId, userId)
        .eq(UserEntity::getStatus, "enabled")
        .eq(UserEntity::getDeleted, 0));
    if (user == null || tenantId == null) {
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
}
