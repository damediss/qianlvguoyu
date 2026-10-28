package com.guoyu.platform.security.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.RoleMenuMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.system.mapper.UserRoleMapper;
import com.guoyu.platform.trainingdepartment.service.EnterpriseFeatureConfigService;
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

  private static final String PLAYER_API_PREFIX = "/api/competition/player/";
  private static final String EXAM_API_PREFIX = "/api/competition/exam/";
  private static final String FORMAL_EXAM_PLAYER_API_PREFIX = "/api/competition/formal-exam/player/";

  private final UserMapper userMapper;
  private final UserRoleMapper userRoleMapper;
  private final RoleMenuMapper roleMenuMapper;
  private final EnterpriseFeatureConfigService enterpriseFeatureConfigService;

  public SaTokenBridgeFilter(
      UserMapper userMapper,
      UserRoleMapper userRoleMapper,
      RoleMenuMapper roleMenuMapper,
      EnterpriseFeatureConfigService enterpriseFeatureConfigService
  ) {
    this.userMapper = userMapper;
    this.userRoleMapper = userRoleMapper;
    this.roleMenuMapper = roleMenuMapper;
    this.enterpriseFeatureConfigService = enterpriseFeatureConfigService;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return uri.startsWith(PLAYER_API_PREFIX)
        || uri.startsWith(EXAM_API_PREFIX)
        || uri.startsWith(FORMAL_EXAM_PLAYER_API_PREFIX);
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

    String token = null;
    try {
      token = StpUtil.getTokenValue();
    } catch (Exception e) {
      log.debug("SaTokenBridgeFilter: failed to get token value: {}", e.getMessage());
    }

    if (token != null) {
      try {
        authenticate(token);
      } catch (Exception ex) {
        log.debug("SaTokenBridgeFilter: Sa-Token auth skipped for {}: {}", request.getRequestURI(), ex.getMessage());
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
    List<String> permissionCodes = roleMenuMapper.selectPermissionCodesByUser(tenantId, userId);
    // Defensive null guard for MyBatis mapper results
    if (roleCodes == null) roleCodes = List.of();
    if (permissionCodes == null) permissionCodes = List.of();
    permissionCodes = enterpriseFeatureConfigService.filterPermissionsForUserEntity(user, permissionCodes);
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        user.getId(),
        user.getTenantId(),
        user.getUsername(),
        user.getDisplayName(),
        user.getUserType(),
        roleCodes,
        permissionCodes,
        user.getEmail()
    );
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
