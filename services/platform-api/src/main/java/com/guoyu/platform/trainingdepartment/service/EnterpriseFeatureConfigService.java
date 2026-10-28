package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.MenuEntity;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.PermissionEntity;
import com.guoyu.platform.system.entity.RoleEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.MenuMapper;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.PermissionMapper;
import com.guoyu.platform.system.mapper.RoleMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.trainingdepartment.dto.EnterpriseFeatureConfigSaveReq;
import com.guoyu.platform.trainingdepartment.entity.TrainEnterpriseFeatureConfigEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainEnterpriseFeatureConfigMapper;
import com.guoyu.platform.trainingdepartment.vo.EnterpriseFeatureConfigVO;
import com.guoyu.platform.trainingdepartment.vo.EnterpriseFeatureOptionsVO;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 企业功能配置：菜单白名单 + 可授角色包；对企业账号的菜单/权限做裁剪。
 */
@Service
@Slf4j
public class EnterpriseFeatureConfigService {

  private static final String DEFAULT_ENTERPRISE_ROLE = "TRAIN_ENTERPRISE";
  private static final Set<String> BLOCKED_ROLE_CODES = Set.of("PLATFORM_ADMIN", "SUPER_ADMIN");
  private static final Set<String> ALWAYS_KEEP_PERMISSION_PREFIXES = Set.of(
      "train:company:"
  );

  private final TrainEnterpriseFeatureConfigMapper configMapper;
  private final TrainingOperatorScopeService scopeService;
  private final OrgMapper orgMapper;
  private final UserMapper userMapper;
  private final MenuMapper menuMapper;
  private final PermissionMapper permissionMapper;
  private final RoleMapper roleMapper;
  private final ObjectMapper objectMapper;

  public EnterpriseFeatureConfigService(
      TrainEnterpriseFeatureConfigMapper configMapper,
      TrainingOperatorScopeService scopeService,
      OrgMapper orgMapper,
      UserMapper userMapper,
      MenuMapper menuMapper,
      PermissionMapper permissionMapper,
      RoleMapper roleMapper,
      ObjectMapper objectMapper) {
    this.configMapper = configMapper;
    this.scopeService = scopeService;
    this.orgMapper = orgMapper;
    this.userMapper = userMapper;
    this.menuMapper = menuMapper;
    this.permissionMapper = permissionMapper;
    this.roleMapper = roleMapper;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public EnterpriseFeatureOptionsVO options() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<MenuEntity> menus = menuMapper.selectList(new LambdaQueryWrapper<MenuEntity>()
        .eq(MenuEntity::getTenantId, tenantId)
        .eq(MenuEntity::getDeleted, 0)
        .eq(MenuEntity::getStatus, "enabled")
        .orderByAsc(MenuEntity::getSortOrder)
        .orderByAsc(MenuEntity::getId));
    List<EnterpriseFeatureOptionsVO.MenuOption> menuOptions = menus.stream()
        .filter(this::isFeatureConfigurableMenu)
        .map(m -> new EnterpriseFeatureOptionsVO.MenuOption(
            m.getId(), m.getParentId(), m.getCode(), m.getName(), m.getMenuType()))
        .toList();

    List<RoleEntity> roles = roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>()
        .eq(RoleEntity::getTenantId, tenantId)
        .eq(RoleEntity::getDeleted, 0)
        .eq(RoleEntity::getStatus, "enabled")
        .orderByAsc(RoleEntity::getId));
    List<EnterpriseFeatureOptionsVO.RoleOption> roleOptions = roles.stream()
        .filter(r -> r.getCode() == null
            || !BLOCKED_ROLE_CODES.contains(r.getCode().toUpperCase(Locale.ROOT)))
        .map(r -> new EnterpriseFeatureOptionsVO.RoleOption(
            r.getId(),
            r.getCode(),
            r.getName(),
            DEFAULT_ENTERPRISE_ROLE.equalsIgnoreCase(r.getCode())))
        .toList();
    return new EnterpriseFeatureOptionsVO(menuOptions, roleOptions);
  }

  @Transactional(readOnly = true)
  public EnterpriseFeatureConfigVO get(Long enterpriseId) {
    scopeService.assertCanManageEnterprise(enterpriseId);
    OrgEntity company = requireCompany(enterpriseId);
    TrainEnterpriseFeatureConfigEntity entity = find(enterpriseId);
    if (entity == null) {
      return new EnterpriseFeatureConfigVO(
          enterpriseId, company.getName(), 0L, List.of(), List.of(), false, false);
    }
    return toVO(company, entity);
  }

  @Transactional
  public EnterpriseFeatureConfigVO save(Long enterpriseId, EnterpriseFeatureConfigSaveReq req) {
    scopeService.assertCanManageEnterprise(enterpriseId);
    if (scopeService.isEnterpriseScoped() && !scopeService.isPlatformAdmin()) {
      // 企业管理员只能在已有上限内配子账号角色，不能抬高本企功能天花板
      throw new BusinessException(ErrorCode.FORBIDDEN, "企业账号不可修改功能上限，请联系机构或平台管理员");
    }
    OrgEntity company = requireCompany(enterpriseId);
    List<String> menuCodes = req.menuCodes() == null ? List.of() : req.menuCodes().stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .distinct()
        .toList();
    List<Long> roleIds = req.roleIds() == null ? List.of() : req.roleIds().stream()
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    validateRoleIds(roleIds);

    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainingOperatorScopeService.Scope scope = scopeService.currentScope();
    Long configurerOrgId = scopeService.isPlatformAdmin() ? 0L
        : (scope.orgId() == null ? 0L : scope.orgId());

    TrainEnterpriseFeatureConfigEntity entity = find(enterpriseId);
    if (entity == null) {
      entity = new TrainEnterpriseFeatureConfigEntity();
      entity.setId(IdWorker.getId());
      entity.setTenantId(tenantId);
      entity.setEnterpriseId(enterpriseId);
      entity.setDeleted(0);
      entity.setConfigurerOrgId(configurerOrgId);
      entity.setMenuCodesJson(writeJson(menuCodes));
      entity.setRoleIdsJson(writeJson(roleIds));
      entity.setConstrainEnabled(Boolean.TRUE.equals(req.constrainEnabled()) ? 1 : 0);
      configMapper.insert(entity);
    } else {
      // 后写覆盖
      entity.setConfigurerOrgId(configurerOrgId);
      entity.setMenuCodesJson(writeJson(menuCodes));
      entity.setRoleIdsJson(writeJson(roleIds));
      entity.setConstrainEnabled(Boolean.TRUE.equals(req.constrainEnabled()) ? 1 : 0);
      configMapper.updateById(entity);
    }
    log.info("enterprise feature config saved enterpriseId={} constrain={} menus={} roles={} by={}",
        enterpriseId, req.constrainEnabled(), menuCodes.size(), roleIds.size(),
        SecurityContext.currentUser().getUserId());
    return toVO(company, entity);
  }

  /** 对企业用户权限列表按功能配置裁剪；非企业或无约束时原样返回 */
  @Transactional(readOnly = true)
  public List<String> filterPermissionsForUser(Long userId, List<String> permissionCodes) {
    if (permissionCodes == null || permissionCodes.isEmpty() || userId == null) {
      return permissionCodes == null ? List.of() : permissionCodes;
    }
    EffectiveConstraint constraint = resolveConstraintForUser(userId);
    if (constraint == null) {
      return permissionCodes;
    }
    Set<String> allowed = resolveAllowedPermissionCodes(constraint.tenantId(), constraint.menuCodes());
    ALWAYS_KEEP_PERMISSION_PREFIXES.forEach(prefix ->
        permissionCodes.stream().filter(p -> p != null && p.startsWith(prefix)).forEach(allowed::add));
    // 非功能目录下的权限（如系统管理）不在目录内，对已拥有的保留
    Set<String> catalogPermissionCodes = resolveCatalogPermissionCodes(constraint.tenantId());
    return permissionCodes.stream()
        .filter(Objects::nonNull)
        .filter(code -> !catalogPermissionCodes.contains(code) || allowed.contains(code))
        .toList();
  }

  /** 对企业用户菜单按功能配置裁剪 */
  @Transactional(readOnly = true)
  public List<MenuEntity> filterMenusForUser(Long userId, List<MenuEntity> menus) {
    if (menus == null || menus.isEmpty() || userId == null) {
      return menus == null ? List.of() : menus;
    }
    EffectiveConstraint constraint = resolveConstraintForUser(userId);
    if (constraint == null) {
      return menus;
    }
    Set<String> allowedCodes = new HashSet<>(constraint.menuCodes());
    // 保留允许菜单及其祖先；非功能目录菜单原样保留
    Set<String> catalogCodes = menus.stream()
        .filter(this::isFeatureConfigurableMenu)
        .map(MenuEntity::getCode)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    Set<Long> keepIds = new HashSet<>();
    for (MenuEntity menu : menus) {
      if (menu.getCode() == null || !catalogCodes.contains(menu.getCode()) || allowedCodes.contains(menu.getCode())) {
        keepIds.add(menu.getId());
      }
    }
    // 补祖先已在 Auth 侧处理；此处再确保允许菜单的父级若在列表中则保留
    boolean changed = true;
    while (changed) {
      changed = false;
      for (MenuEntity menu : menus) {
        if (keepIds.contains(menu.getId()) && menu.getParentId() != null && menu.getParentId() > 0) {
          if (keepIds.add(menu.getParentId())) {
            changed = true;
          }
        }
      }
    }
    return menus.stream().filter(m -> keepIds.contains(m.getId())).toList();
  }

  /** 开户可选角色：全局可授 ∩ 企业配置角色包 */
  @Transactional(readOnly = true)
  public List<Long> filterAssignableRoleIds(Long enterpriseId, List<Long> candidateRoleIds) {
    if (candidateRoleIds == null || candidateRoleIds.isEmpty()) {
      return List.of();
    }
    EffectiveConstraint constraint = resolveConstraintForEnterprise(enterpriseId);
    if (constraint == null) {
      return candidateRoleIds;
    }
    Set<Long> allowed = new HashSet<>(constraint.roleIds());
    if (allowed.isEmpty()) {
      return List.of();
    }
    return candidateRoleIds.stream().filter(allowed::contains).toList();
  }

  @Transactional(readOnly = true)
  public void assertRolesAssignable(Long enterpriseId, List<Long> roleIds) {
    if (roleIds == null || roleIds.isEmpty()) {
      return;
    }
    EffectiveConstraint constraint = resolveConstraintForEnterprise(enterpriseId);
    if (constraint == null) {
      return;
    }
    Set<Long> allowed = new HashSet<>(constraint.roleIds());
    for (Long roleId : roleIds) {
      if (!allowed.contains(roleId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "所选角色超出该企业功能配置的可授角色包");
      }
    }
  }

  private EffectiveConstraint resolveConstraintForUser(Long userId) {
    UserEntity user = userMapper.selectById(userId);
    if (user == null || user.getOrgId() == null) {
      return null;
    }
    if (scopeService.isPlatformAdmin()) {
      // 当前操作者是超管时不过滤；这里用于登录用户自身权限裁剪，需看目标用户组织
    }
    OrgEntity org = orgMapper.selectById(user.getOrgId());
    if (org == null || !"company".equals(org.getOrgType())) {
      return null;
    }
    return resolveConstraintForEnterprise(user.getOrgId());
  }

  private EffectiveConstraint resolveConstraintForEnterprise(Long enterpriseId) {
    TrainEnterpriseFeatureConfigEntity entity = find(enterpriseId);
    if (entity == null || entity.getConstrainEnabled() == null || entity.getConstrainEnabled() != 1) {
      return null;
    }
    List<String> menuCodes = readStringList(entity.getMenuCodesJson());
    List<Long> roleIds = readLongList(entity.getRoleIdsJson());
    return new EffectiveConstraint(entity.getTenantId(), menuCodes, roleIds);
  }

  private TrainEnterpriseFeatureConfigEntity find(Long enterpriseId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    return configMapper.selectOne(new LambdaQueryWrapper<TrainEnterpriseFeatureConfigEntity>()
        .eq(TrainEnterpriseFeatureConfigEntity::getTenantId, tenantId)
        .eq(TrainEnterpriseFeatureConfigEntity::getEnterpriseId, enterpriseId)
        .eq(TrainEnterpriseFeatureConfigEntity::getDeleted, 0)
        .last("LIMIT 1"));
  }

  /** 无 SecurityContext 时（过滤器重建 principal）按用户 tenant 查 */
  @Transactional(readOnly = true)
  public List<String> filterPermissionsForUserEntity(UserEntity user, List<String> permissionCodes) {
    if (user == null || user.getOrgId() == null || permissionCodes == null) {
      return permissionCodes == null ? List.of() : permissionCodes;
    }
    OrgEntity org = orgMapper.selectById(user.getOrgId());
    if (org == null || !"company".equals(org.getOrgType())) {
      return permissionCodes;
    }
    TrainEnterpriseFeatureConfigEntity entity = configMapper.selectOne(
        new LambdaQueryWrapper<TrainEnterpriseFeatureConfigEntity>()
            .eq(TrainEnterpriseFeatureConfigEntity::getTenantId, user.getTenantId())
            .eq(TrainEnterpriseFeatureConfigEntity::getEnterpriseId, user.getOrgId())
            .eq(TrainEnterpriseFeatureConfigEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (entity == null || entity.getConstrainEnabled() == null || entity.getConstrainEnabled() != 1) {
      return permissionCodes;
    }
    List<String> menuCodes = readStringList(entity.getMenuCodesJson());
    Set<String> allowed = resolveAllowedPermissionCodes(user.getTenantId(), menuCodes);
    ALWAYS_KEEP_PERMISSION_PREFIXES.forEach(prefix ->
        permissionCodes.stream().filter(p -> p != null && p.startsWith(prefix)).forEach(allowed::add));
    Set<String> catalogPermissionCodes = resolveCatalogPermissionCodes(user.getTenantId());
    return permissionCodes.stream()
        .filter(Objects::nonNull)
        .filter(code -> !catalogPermissionCodes.contains(code) || allowed.contains(code))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<MenuEntity> filterMenusForUserEntity(UserEntity user, List<MenuEntity> menus) {
    if (user == null || user.getOrgId() == null || menus == null || menus.isEmpty()) {
      return menus == null ? List.of() : menus;
    }
    OrgEntity org = orgMapper.selectById(user.getOrgId());
    if (org == null || !"company".equals(org.getOrgType())) {
      return menus;
    }
    TrainEnterpriseFeatureConfigEntity entity = configMapper.selectOne(
        new LambdaQueryWrapper<TrainEnterpriseFeatureConfigEntity>()
            .eq(TrainEnterpriseFeatureConfigEntity::getTenantId, user.getTenantId())
            .eq(TrainEnterpriseFeatureConfigEntity::getEnterpriseId, user.getOrgId())
            .eq(TrainEnterpriseFeatureConfigEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (entity == null || entity.getConstrainEnabled() == null || entity.getConstrainEnabled() != 1) {
      return menus;
    }
    Set<String> allowedCodes = new HashSet<>(readStringList(entity.getMenuCodesJson()));
    Set<String> catalogCodes = menus.stream()
        .filter(this::isFeatureConfigurableMenu)
        .map(MenuEntity::getCode)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    Set<Long> keepIds = new HashSet<>();
    for (MenuEntity menu : menus) {
      if (menu.getCode() == null || !catalogCodes.contains(menu.getCode())
          || allowedCodes.contains(menu.getCode())) {
        keepIds.add(menu.getId());
      }
    }
    boolean changed = true;
    while (changed) {
      changed = false;
      for (MenuEntity menu : menus) {
        if (keepIds.contains(menu.getId()) && menu.getParentId() != null && menu.getParentId() > 0) {
          if (keepIds.add(menu.getParentId())) {
            changed = true;
          }
        }
      }
    }
    return menus.stream().filter(m -> keepIds.contains(m.getId())).toList();
  }

  private Set<String> resolveAllowedPermissionCodes(Long tenantId, List<String> menuCodes) {
    if (menuCodes == null || menuCodes.isEmpty()) {
      return new HashSet<>();
    }
    List<MenuEntity> menus = menuMapper.selectList(new LambdaQueryWrapper<MenuEntity>()
        .eq(MenuEntity::getTenantId, tenantId)
        .eq(MenuEntity::getDeleted, 0)
        .in(MenuEntity::getCode, menuCodes));
    if (menus.isEmpty()) {
      return new HashSet<>();
    }
    Set<Long> menuIds = menus.stream().map(MenuEntity::getId).collect(Collectors.toSet());
    List<PermissionEntity> perms = permissionMapper.selectList(new LambdaQueryWrapper<PermissionEntity>()
        .eq(PermissionEntity::getTenantId, tenantId)
        .eq(PermissionEntity::getDeleted, 0)
        .eq(PermissionEntity::getStatus, "enabled")
        .in(PermissionEntity::getMenuId, menuIds));
    return perms.stream().map(PermissionEntity::getCode).filter(Objects::nonNull)
        .collect(Collectors.toCollection(HashSet::new));
  }

  private Set<String> resolveCatalogPermissionCodes(Long tenantId) {
    List<MenuEntity> menus = menuMapper.selectList(new LambdaQueryWrapper<MenuEntity>()
        .eq(MenuEntity::getTenantId, tenantId)
        .eq(MenuEntity::getDeleted, 0));
    Set<Long> catalogMenuIds = menus.stream()
        .filter(this::isFeatureConfigurableMenu)
        .map(MenuEntity::getId)
        .collect(Collectors.toSet());
    if (catalogMenuIds.isEmpty()) {
      return Set.of();
    }
    return permissionMapper.selectList(new LambdaQueryWrapper<PermissionEntity>()
            .eq(PermissionEntity::getTenantId, tenantId)
            .eq(PermissionEntity::getDeleted, 0)
            .in(PermissionEntity::getMenuId, catalogMenuIds))
        .stream()
        .map(PermissionEntity::getCode)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private boolean isFeatureConfigurableMenu(MenuEntity menu) {
    if (menu == null || !StringUtils.hasText(menu.getCode())) {
      return false;
    }
    String code = menu.getCode();
    if (code.startsWith("system_") || code.startsWith("sys_")) {
      return false;
    }
    // 培训及相关业务菜单可配置；排除纯系统管理
    return code.startsWith("train_")
        || code.startsWith("px")
        || code.startsWith("company_")
        || (menu.getPermissionCode() != null && menu.getPermissionCode().startsWith("train:"))
        || (menu.getPermissionCode() != null && menu.getPermissionCode().startsWith("px"));
  }

  private void validateRoleIds(List<Long> roleIds) {
    if (roleIds.isEmpty()) {
      return;
    }
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<RoleEntity> roles = roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>()
        .eq(RoleEntity::getTenantId, tenantId)
        .in(RoleEntity::getId, roleIds)
        .eq(RoleEntity::getDeleted, 0));
    if (roles.size() != new HashSet<>(roleIds).size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "角色不存在");
    }
    for (RoleEntity role : roles) {
      if (role.getCode() != null
          && BLOCKED_ROLE_CODES.contains(role.getCode().toUpperCase(Locale.ROOT))) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "不可将平台管理员角色纳入企业可授角色包");
      }
    }
  }

  private OrgEntity requireCompany(Long enterpriseId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    OrgEntity entity = orgMapper.selectOne(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getId, enterpriseId)
        .eq(OrgEntity::getOrgType, "company")
        .eq(OrgEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "企业不存在");
    }
    return entity;
  }

  private EnterpriseFeatureConfigVO toVO(OrgEntity company, TrainEnterpriseFeatureConfigEntity entity) {
    return new EnterpriseFeatureConfigVO(
        company.getId(),
        company.getName(),
        entity.getConfigurerOrgId(),
        readStringList(entity.getMenuCodesJson()),
        readLongList(entity.getRoleIdsJson()),
        entity.getConstrainEnabled() != null && entity.getConstrainEnabled() == 1,
        true);
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "配置序列化失败");
    }
  }

  private List<String> readStringList(String json) {
    if (!StringUtils.hasText(json)) {
      return List.of();
    }
    try {
      List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
      return list == null ? List.of() : list;
    } catch (Exception e) {
      return List.of();
    }
  }

  private List<Long> readLongList(String json) {
    if (!StringUtils.hasText(json)) {
      return List.of();
    }
    try {
      List<Long> list = objectMapper.readValue(json, new TypeReference<List<Long>>() {});
      return list == null ? List.of() : list;
    } catch (Exception e) {
      return List.of();
    }
  }

  private record EffectiveConstraint(Long tenantId, List<String> menuCodes, List<Long> roleIds) {}
}
