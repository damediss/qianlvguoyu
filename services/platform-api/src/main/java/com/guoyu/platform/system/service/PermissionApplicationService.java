package com.guoyu.platform.system.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.MenuEntity;
import com.guoyu.platform.system.entity.PermissionEntity;
import com.guoyu.platform.system.entity.RoleEntity;
import com.guoyu.platform.system.entity.RolePermissionEntity;
import com.guoyu.platform.system.mapper.MenuMapper;
import com.guoyu.platform.system.mapper.PermissionMapper;
import com.guoyu.platform.system.mapper.RoleMapper;
import com.guoyu.platform.system.mapper.RolePermissionRelationMapper;
import com.guoyu.platform.system.vo.PermissionItemVO;
import com.guoyu.platform.system.vo.PermissionMatrixVO;
import com.guoyu.platform.system.vo.RoleBriefVO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class PermissionApplicationService {

  private final PermissionMapper permissionMapper;
  private final MenuMapper menuMapper;
  private final RoleMapper roleMapper;
  private final RolePermissionRelationMapper rolePermissionRelationMapper;

  public PermissionApplicationService(
      PermissionMapper permissionMapper,
      MenuMapper menuMapper,
      RoleMapper roleMapper,
      RolePermissionRelationMapper rolePermissionRelationMapper
  ) {
    this.permissionMapper = permissionMapper;
    this.menuMapper = menuMapper;
    this.roleMapper = roleMapper;
    this.rolePermissionRelationMapper = rolePermissionRelationMapper;
  }

  @Transactional(readOnly = true)
  public List<PermissionMatrixVO> matrix() {
    Long tenantId = SecurityContext.currentUser().getTenantId();

    // 1. Load all enabled menus
    List<MenuEntity> menus = menuMapper.selectList(new LambdaQueryWrapper<MenuEntity>()
        .eq(MenuEntity::getTenantId, tenantId)
        .eq(MenuEntity::getDeleted, 0)
        .eq(MenuEntity::getStatus, "enabled")
        .orderByAsc(MenuEntity::getSortOrder));

    // 2. Load all enabled permissions
    List<PermissionEntity> permissions = permissionMapper.selectList(new LambdaQueryWrapper<PermissionEntity>()
        .eq(PermissionEntity::getTenantId, tenantId)
        .eq(PermissionEntity::getDeleted, 0)
        .eq(PermissionEntity::getStatus, "enabled"));

    // 3. Load all role-permission relations
    List<RolePermissionEntity> rolePermissions = rolePermissionRelationMapper.selectList(
        new LambdaQueryWrapper<RolePermissionEntity>()
            .eq(RolePermissionEntity::getTenantId, tenantId)
            .eq(RolePermissionEntity::getDeleted, 0));

    // 4. Load all enabled roles
    List<RoleEntity> roles = roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>()
        .eq(RoleEntity::getTenantId, tenantId)
        .eq(RoleEntity::getDeleted, 0)
        .eq(RoleEntity::getStatus, "enabled"));

    // Build role lookup
    Map<Long, RoleEntity> roleById = roles.stream()
        .collect(Collectors.toMap(RoleEntity::getId, r -> r));

    // Build permissionId -> bound roles map
    Map<Long, List<RoleBriefVO>> rolesByPermId = new HashMap<>();
    for (RolePermissionEntity rp : rolePermissions) {
      RoleEntity role = roleById.get(rp.getRoleId());
      if (role != null) {
        rolesByPermId
            .computeIfAbsent(rp.getPermissionId(), k -> new ArrayList<>())
            .add(new RoleBriefVO(role.getId(), role.getCode(), role.getName()));
      }
    }

    // Build menu lookup
    Map<Long, MenuEntity> menuById = menus.stream()
        .collect(Collectors.toMap(MenuEntity::getId, m -> m));

    // Group permissions by menuId
    Map<Long, List<PermissionEntity>> permsByMenu = permissions.stream()
        .filter(p -> p.getMenuId() != null)
        .collect(Collectors.groupingBy(PermissionEntity::getMenuId));

    // Build result
    List<PermissionMatrixVO> result = new ArrayList<>();
    for (MenuEntity menu : menus) {
      List<PermissionEntity> menuPerms = permsByMenu.getOrDefault(menu.getId(), List.of());
      if (menuPerms.isEmpty()) {
        continue;
      }
      List<PermissionItemVO> items = menuPerms.stream()
          .map(p -> new PermissionItemVO(
              p.getId(),
              p.getCode(),
              p.getName(),
              p.getAction(),
              p.getStatus(),
              menu.getName(),
              rolesByPermId.getOrDefault(p.getId(), List.of())
          ))
          .toList();
      result.add(new PermissionMatrixVO(
          menu.getId(),
          menu.getName(),
          menu.getCode(),
          menu.getSortOrder(),
          items
      ));
    }

    return result;
  }

  @Transactional(readOnly = true)
  public List<PermissionItemVO> listFlat() {
    Long tenantId = SecurityContext.currentUser().getTenantId();

    List<PermissionEntity> permissions = permissionMapper.selectList(new LambdaQueryWrapper<PermissionEntity>()
        .eq(PermissionEntity::getTenantId, tenantId)
        .eq(PermissionEntity::getDeleted, 0)
        .eq(PermissionEntity::getStatus, "enabled")
        .orderByAsc(PermissionEntity::getCode));

    List<RolePermissionEntity> rolePermissions = rolePermissionRelationMapper.selectList(
        new LambdaQueryWrapper<RolePermissionEntity>()
            .eq(RolePermissionEntity::getTenantId, tenantId)
            .eq(RolePermissionEntity::getDeleted, 0));

    List<RoleEntity> roles = roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>()
        .eq(RoleEntity::getTenantId, tenantId)
        .eq(RoleEntity::getDeleted, 0)
        .eq(RoleEntity::getStatus, "enabled"));

    Map<Long, RoleEntity> roleById = roles.stream()
        .collect(Collectors.toMap(RoleEntity::getId, r -> r));

    Map<Long, List<RoleBriefVO>> rolesByPermId = new HashMap<>();
    for (RolePermissionEntity rp : rolePermissions) {
      RoleEntity role = roleById.get(rp.getRoleId());
      if (role != null) {
        rolesByPermId
            .computeIfAbsent(rp.getPermissionId(), k -> new ArrayList<>())
            .add(new RoleBriefVO(role.getId(), role.getCode(), role.getName()));
      }
    }

    // Build menu name lookup
    List<MenuEntity> menus = menuMapper.selectList(new LambdaQueryWrapper<MenuEntity>()
        .eq(MenuEntity::getTenantId, tenantId)
        .eq(MenuEntity::getDeleted, 0));
    Map<Long, String> menuNameById = menus.stream()
        .collect(Collectors.toMap(MenuEntity::getId, MenuEntity::getName));

    return permissions.stream()
        .map(p -> new PermissionItemVO(
            p.getId(),
            p.getCode(),
            p.getName(),
            p.getAction(),
            p.getStatus(),
            p.getMenuId() != null ? menuNameById.getOrDefault(p.getMenuId(), "") : "",
            rolesByPermId.getOrDefault(p.getId(), List.of())
        ))
        .toList();
  }
}
