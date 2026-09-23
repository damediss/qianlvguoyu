package com.guoyu.platform.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.auth.dto.LoginReq;
import com.guoyu.platform.auth.vo.CurrentUserVO;
import com.guoyu.platform.auth.vo.HomeVO;
import com.guoyu.platform.auth.support.LoginFailureGuard;
import com.guoyu.platform.auth.vo.LoginVO;
import com.guoyu.platform.auth.vo.MenuVO;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.security.PlatformAdminSupport;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.MenuEntity;
import com.guoyu.platform.system.entity.TenantEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.MenuMapper;
import com.guoyu.platform.system.mapper.RoleMenuMapper;
import com.guoyu.platform.system.mapper.TenantMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.system.mapper.UserRoleMapper;
import com.guoyu.platform.trainingdepartment.service.TrainingOperatorScopeService;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Comparator;
import java.util.ArrayList;
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
  private final MenuMapper menuMapper;
  private final PasswordEncoder passwordEncoder;
  private final TrainingOperatorScopeService trainingOperatorScopeService;
  private final LoginFailureGuard loginFailureGuard;
  private final SecureRandom secureRandom = new SecureRandom();

  public AuthApplicationService(
      TenantMapper tenantMapper,
      UserMapper userMapper,
      UserRoleMapper userRoleMapper,
      RoleMenuMapper roleMenuMapper,
      MenuMapper menuMapper,
      PasswordEncoder passwordEncoder,
      TrainingOperatorScopeService trainingOperatorScopeService,
      LoginFailureGuard loginFailureGuard
  ) {
    this.tenantMapper = tenantMapper;
    this.userMapper = userMapper;
    this.userRoleMapper = userRoleMapper;
    this.roleMenuMapper = roleMenuMapper;
    this.menuMapper = menuMapper;
    this.passwordEncoder = passwordEncoder;
    this.trainingOperatorScopeService = trainingOperatorScopeService;
    this.loginFailureGuard = loginFailureGuard;
  }

  @Transactional(readOnly = true)
  public LoginVO login(LoginReq req) {
    // 1. captcha 校验(临时关闭,LoginReq 已移除 captchaToken 字段)
    // captchaService.consumeToken(req.captchaToken());

    // 1.5 登录失败锁定（§3.1）：先判锁定再校验凭据，锁定中即使密码正确也拒绝
    long lockSeconds = loginFailureGuard.remainingLockSeconds(req.tenantCode(), req.username());
    if (lockSeconds > 0) {
      log.warn("登录被锁定拒绝 tenantCode={} username={} remainingSeconds={}",
          req.tenantCode(), req.username(), lockSeconds);
      throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "尝试次数过多，请稍后再试");
    }

    // 2. Original tenant/user/password verification
    TenantEntity tenant = tenantMapper.selectOne(new LambdaQueryWrapper<TenantEntity>()
        .eq(TenantEntity::getCode, req.tenantCode())
        .eq(TenantEntity::getStatus, "enabled")
        .eq(TenantEntity::getDeleted, 0));
    if (tenant == null) {
      loginFailureGuard.recordFailure(req.tenantCode(), req.username());
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid username or password");
    }
    // 登录按 tenantCode 解析出租户后，立即把上下文租户设为该租户 id，再查用户/角色/菜单。
    // 否则浏览器残留的旧 X-Tenant-Id（如租户归一前的 1）会让多租户拦截器把 user 查询过滤为空 → 401。
    // TenantContextFilter 的 finally 会负责清理。
    TenantContextHolder.setTenantId(tenant.getId());

    UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, tenant.getId())
        .eq(UserEntity::getUsername, req.username())
        .eq(UserEntity::getStatus, "enabled")
        .eq(UserEntity::getDeleted, 0));
    if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
      loginFailureGuard.recordFailure(req.tenantCode(), req.username());
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid username or password");
    }
    loginFailureGuard.clear(req.tenantCode(), req.username());

    PlatformUserPrincipal principal = buildPrincipal(user);

    StpUtil.login(user.getId());
    // P0 Direction A: 只存 userId，不存整个 principal（避免 Jackson 反序列化问题）
    // Filter 每次请求时从 DB 重建 principal，保证禁用用户立即失效
    StpUtil.getTokenSession().set("userId", user.getId());
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
    List<MenuEntity> menus;
    if (PlatformAdminSupport.isPlatformAdmin(currentUser.getRoleCodes())) {
      menus = menuMapper.selectList(new LambdaQueryWrapper<MenuEntity>()
          .eq(MenuEntity::getTenantId, currentUser.getTenantId())
          .eq(MenuEntity::getDeleted, 0)
          .eq(MenuEntity::getStatus, "enabled")
          .eq(MenuEntity::getVisible, 1)
          .orderByAsc(MenuEntity::getSortOrder)
          .orderByAsc(MenuEntity::getId));
    } else {
      menus = includeMenuAncestors(
          currentUser.getTenantId(),
          roleMenuMapper.selectMenusByUser(currentUser.getTenantId(), currentUser.getUserId()));
    }
    return buildMenuTree(menus);
  }

  /** 补齐父级目录，避免仅有子菜单授权时侧栏树形结构断裂 */
  private List<MenuEntity> includeMenuAncestors(Long tenantId, List<MenuEntity> menus) {
    Map<Long, MenuEntity> byId = new LinkedHashMap<>();
    for (MenuEntity menu : menus) {
      byId.put(menu.getId(), menu);
    }
    boolean changed = true;
    while (changed) {
      changed = false;
      for (MenuEntity menu : new ArrayList<>(byId.values())) {
        Long parentId = menu.getParentId();
        if (parentId == null || parentId <= 0 || byId.containsKey(parentId)) {
          continue;
        }
        MenuEntity parent = menuMapper.selectOne(new LambdaQueryWrapper<MenuEntity>()
            .eq(MenuEntity::getTenantId, tenantId)
            .eq(MenuEntity::getId, parentId)
            .eq(MenuEntity::getDeleted, 0)
            .eq(MenuEntity::getStatus, "enabled")
            .eq(MenuEntity::getVisible, 1));
        if (parent != null) {
          byId.put(parent.getId(), parent);
          changed = true;
        }
      }
    }
    return new ArrayList<>(byId.values());
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
        permissions,
        user.getEmail()
    );
  }

  private CurrentUserVO toCurrentUser(PlatformUserPrincipal principal) {
    TrainingOperatorScopeService.Scope scope = trainingOperatorScopeService.scopeForUser(principal.getUserId());
    // 平台管理员所属组织即使是 company/master，对外仍按机构视角，避免前端误收窄数据
    String loginRole = trainingOperatorScopeService.isPlatformAdmin(principal) ? "org" : scope.loginRole();
    return new CurrentUserVO(
        principal.getUserId(),
        principal.getTenantId(),
        principal.getUsername(),
        principal.getDisplayName(),
        principal.getUserType(),
        scope.orgId(),
        scope.orgType(),
        loginRole,
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
    Comparator<MenuVO> comparator = Comparator
        .comparing(MenuVO::getSortOrder, Comparator.nullsLast(Integer::compareTo))
        .thenComparing(MenuVO::getId);
    nodeMap.values().forEach(node -> node.getChildren().sort(comparator));
    return nodeMap.values().stream()
        .filter(node -> node.getParentId() == null || node.getParentId() == 0 || !nodeMap.containsKey(node.getParentId()))
        .sorted(comparator)
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
    vo.setExpanded(entity.getExpanded());
    return vo;
  }

  /** 查询所有启用状态租户（供登录页下拉框使用，无需认证） */
  @Transactional(readOnly = true)
  public List<Map<String, String>> listTenants() {
    List<TenantEntity> tenants = tenantMapper.selectList(new LambdaQueryWrapper<TenantEntity>()
        .eq(TenantEntity::getStatus, "enabled")
        .eq(TenantEntity::getDeleted, 0)
        .orderByAsc(TenantEntity::getId));
    return tenants.stream()
        .map(t -> Map.of("code", t.getCode(), "name", t.getName()))
        .toList();
  }

  private String createOpaqueRefreshToken() {
    byte[] bytes = new byte[48];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
