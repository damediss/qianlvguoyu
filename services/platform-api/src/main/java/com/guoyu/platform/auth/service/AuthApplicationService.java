package com.guoyu.platform.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.auth.dto.LoginReq;
import com.guoyu.platform.auth.vo.CurrentUserVO;
import com.guoyu.platform.auth.vo.HomeVO;
import com.guoyu.platform.auth.vo.LoginVO;
import com.guoyu.platform.auth.vo.MenuVO;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.MenuEntity;
import com.guoyu.platform.system.entity.TenantEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.RoleMenuMapper;
import com.guoyu.platform.system.mapper.TenantMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.system.mapper.UserRoleMapper;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AuthApplicationService {

  private final TenantMapper tenantMapper;
  private final UserMapper userMapper;
  private final UserRoleMapper userRoleMapper;
  private final RoleMenuMapper roleMenuMapper;
  private final PasswordEncoder passwordEncoder;
  private final SecureRandom secureRandom = new SecureRandom();

  public AuthApplicationService(
      TenantMapper tenantMapper,
      UserMapper userMapper,
      UserRoleMapper userRoleMapper,
      RoleMenuMapper roleMenuMapper,
      PasswordEncoder passwordEncoder
  ) {
    this.tenantMapper = tenantMapper;
    this.userMapper = userMapper;
    this.userRoleMapper = userRoleMapper;
    this.roleMenuMapper = roleMenuMapper;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(readOnly = true)
  public LoginVO login(LoginReq req) {
    TenantEntity tenant = tenantMapper.selectOne(new LambdaQueryWrapper<TenantEntity>()
        .eq(TenantEntity::getCode, req.tenantCode())
        .eq(TenantEntity::getStatus, "enabled")
        .eq(TenantEntity::getDeleted, 0));
    if (tenant == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid username or password");
    }

    UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, tenant.getId())
        .eq(UserEntity::getUsername, req.username())
        .eq(UserEntity::getStatus, "enabled")
        .eq(UserEntity::getDeleted, 0));
    if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid username or password");
    }

    PlatformUserPrincipal principal = buildPrincipal(user);

    StpUtil.login(user.getId());
    StpUtil.getTokenSession().set("principal", principal);
    String accessToken = StpUtil.getTokenValue();

    String refreshToken = createOpaqueRefreshToken();
    return new LoginVO(
        accessToken,
        refreshToken,
        StpUtil.getTokenTimeout(),
        tenant.getId(),
        toCurrentUser(principal)
    );
  }

  public void logout() {
    StpUtil.logout();
  }

  public CurrentUserVO me() {
    return toCurrentUser(SecurityContext.currentUser());
  }

  public List<MenuVO> menus() {
    PlatformUserPrincipal currentUser = SecurityContext.currentUser();
    List<MenuEntity> menus = roleMenuMapper.selectMenusByUser(
        currentUser.getTenantId(),
        currentUser.getUserId()
    );
    return buildMenuTree(menus);
  }

  public HomeVO home() {
    PlatformUserPrincipal currentUser = SecurityContext.currentUser();
    if ("student".equals(currentUser.getUserType())) {
      return new HomeVO("/student/courses", "学员学习首页");
    }
    return new HomeVO("/dashboard", "管理概览页");
  }

  private PlatformUserPrincipal buildPrincipal(UserEntity user) {
    List<String> roles = userRoleMapper.selectRoleCodes(user.getTenantId(), user.getId());
    List<String> permissions = roleMenuMapper.selectPermissionCodesByUser(user.getTenantId(), user.getId());
    if (roles == null) roles = List.of();
    if (permissions == null) permissions = List.of();
    return new PlatformUserPrincipal(
        user.getId(),
        user.getTenantId(),
        user.getUsername(),
        user.getDisplayName(),
        user.getUserType(),
        roles,
        permissions
    );
  }

  private CurrentUserVO toCurrentUser(PlatformUserPrincipal principal) {
    return new CurrentUserVO(
        principal.getUserId(),
        principal.getTenantId(),
        principal.getUsername(),
        principal.getDisplayName(),
        principal.getUserType(),
        principal.getRoleCodes(),
        principal.getPermissionCodes()
    );
  }

  private List<MenuVO> buildMenuTree(List<MenuEntity> menuEntities) {
    Map<Long, MenuVO> nodeMap = new LinkedHashMap<>();
    for (MenuEntity entity : menuEntities) {
      MenuVO node = toMenuVO(entity);
      nodeMap.put(node.getId(), node);
    }
    for (MenuVO node : nodeMap.values()) {
      MenuVO parent = nodeMap.get(node.getParentId());
      if (parent != null) {
        parent.getChildren().add(node);
      }
    }
    return nodeMap.values().stream()
        .filter(node -> node.getParentId() == null || node.getParentId() == 0 || !nodeMap.containsKey(node.getParentId()))
        .sorted(Comparator.comparing(MenuVO::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
        .toList();
  }

  private MenuVO toMenuVO(MenuEntity entity) {
    MenuVO vo = new MenuVO();
    vo.setId(entity.getId());
    vo.setParentId(entity.getParentId());
    vo.setMenuType(entity.getMenuType());
    vo.setCode(entity.getCode());
    vo.setName(entity.getName());
    vo.setPath(entity.getPath());
    vo.setComponent(entity.getComponent());
    vo.setPermissionCode(entity.getPermissionCode());
    vo.setIcon(entity.getIcon());
    vo.setSortOrder(entity.getSortOrder());
    return vo;
  }

  private String createOpaqueRefreshToken() {
    byte[] bytes = new byte[48];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
