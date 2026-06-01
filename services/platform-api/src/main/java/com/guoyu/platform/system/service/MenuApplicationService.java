package com.guoyu.platform.system.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.dto.MenuBatchVisibleReq;
import com.guoyu.platform.system.dto.MenuCreateReq;
import com.guoyu.platform.system.dto.MenuUpdateReq;
import com.guoyu.platform.system.entity.MenuEntity;
import com.guoyu.platform.system.entity.RoleEntity;
import com.guoyu.platform.system.entity.RoleMenuEntity;
import com.guoyu.platform.system.mapper.MenuMapper;
import com.guoyu.platform.system.mapper.RoleMapper;
import com.guoyu.platform.system.mapper.RoleMenuRelationMapper;
import com.guoyu.platform.system.vo.MenuTreeVO;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class MenuApplicationService {

  private final MenuMapper menuMapper;
  private final RoleMapper roleMapper;
  private final RoleMenuRelationMapper roleMenuRelationMapper;

  public MenuApplicationService(MenuMapper menuMapper, RoleMapper roleMapper,
      RoleMenuRelationMapper roleMenuRelationMapper) {
    this.menuMapper = menuMapper;
    this.roleMapper = roleMapper;
    this.roleMenuRelationMapper = roleMenuRelationMapper;
  }

  @Transactional(readOnly = true)
  public List<MenuTreeVO> tree() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<MenuEntity> menus = menuMapper.selectList(new LambdaQueryWrapper<MenuEntity>()
        .eq(MenuEntity::getTenantId, tenantId)
        .eq(MenuEntity::getDeleted, 0)
        .orderByAsc(MenuEntity::getSortOrder)
        .orderByAsc(MenuEntity::getId));
    return buildTree(menus);
  }

  @Transactional
  public MenuTreeVO create(MenuCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    validateParent(tenantId, req.parentId(), null);
    ensureCodeAvailable(tenantId, req.code());

    MenuEntity entity = new MenuEntity();
    entity.setTenantId(tenantId);
    entity.setParentId(req.parentId());
    entity.setMenuType(req.menuType());
    entity.setCode(req.code());
    entity.setName(req.name());
    entity.setPath(req.path());
    entity.setComponent(req.component());
    entity.setPermissionCode(req.permissionCode());
    entity.setIcon(req.icon());
    entity.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    entity.setVisible(req.visible() == null ? 1 : req.visible());
    entity.setStatus(StringUtils.hasText(req.status()) ? req.status() : "enabled");
    entity.setDeleted(0);
    menuMapper.insert(entity);

    // Auto-bind new menu to PLATFORM_ADMIN role so it appears in the sidebar
    bindToAdminRole(tenantId, entity.getId());

    return toTreeVO(entity);
  }

  private void bindToAdminRole(Long tenantId, Long menuId) {
    Long adminRoleId = findAdminRoleId(tenantId);
    if (adminRoleId == null) {
      log.warn("PLATFORM_ADMIN role not found in tenant {}, skipping auto-bind for menu {}", tenantId, menuId);
      return;
    }
    boolean alreadyBound = roleMenuRelationMapper.exists(new LambdaQueryWrapper<RoleMenuEntity>()
        .eq(RoleMenuEntity::getTenantId, tenantId)
        .eq(RoleMenuEntity::getRoleId, adminRoleId)
        .eq(RoleMenuEntity::getMenuId, menuId)
        .eq(RoleMenuEntity::getDeleted, 0));
    if (alreadyBound) return;

    RoleMenuEntity binding = new RoleMenuEntity();
    binding.setTenantId(tenantId);
    binding.setRoleId(adminRoleId);
    binding.setMenuId(menuId);
    binding.setDeleted(0);
    roleMenuRelationMapper.insert(binding);
  }

  private Long findAdminRoleId(Long tenantId) {
    return roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>()
            .eq(RoleEntity::getTenantId, tenantId)
            .eq(RoleEntity::getCode, "PLATFORM_ADMIN")
            .eq(RoleEntity::getDeleted, 0))
        .stream()
        .findFirst()
        .map(RoleEntity::getId)
        .orElse(null);
  }

  @Transactional
  public MenuTreeVO update(Long id, MenuUpdateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    MenuEntity entity = getExisting(tenantId, id);
    validateParent(tenantId, req.parentId(), id);

    entity.setParentId(req.parentId());
    entity.setMenuType(req.menuType());
    entity.setName(req.name());
    entity.setPath(req.path());
    entity.setComponent(req.component());
    entity.setPermissionCode(req.permissionCode());
    entity.setIcon(req.icon());
    entity.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    entity.setVisible(req.visible() == null ? 1 : req.visible());
    entity.setStatus(req.status());
    menuMapper.updateById(entity);
    return toTreeVO(entity);
  }

  @Transactional
  public void batchUpdateVisible(MenuBatchVisibleReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<MenuEntity> entities = menuMapper.selectList(new LambdaQueryWrapper<MenuEntity>()
        .in(MenuEntity::getId, req.ids())
        .eq(MenuEntity::getTenantId, tenantId)
        .eq(MenuEntity::getDeleted, 0));
    if (entities.size() != req.ids().size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Some menus not found or do not belong to current tenant");
    }
    for (MenuEntity entity : entities) {
      entity.setVisible(req.visible());
      menuMapper.updateById(entity);
    }
    log.info("Batch updated visible to {} for {} menus in tenant {}", req.visible(), entities.size(), tenantId);
  }

  @Transactional
  public void delete(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    MenuEntity entity = getExisting(tenantId, id);
    boolean hasChildren = menuMapper.exists(new LambdaQueryWrapper<MenuEntity>()
        .eq(MenuEntity::getTenantId, tenantId)
        .eq(MenuEntity::getParentId, id)
        .eq(MenuEntity::getDeleted, 0));
    if (hasChildren) {
      throw new BusinessException(ErrorCode.CONFLICT, "Menu has child nodes");
    }
    boolean usedByRoles = roleMenuRelationMapper.exists(new LambdaQueryWrapper<RoleMenuEntity>()
        .eq(RoleMenuEntity::getTenantId, tenantId)
        .eq(RoleMenuEntity::getMenuId, id)
        .eq(RoleMenuEntity::getDeleted, 0));
    if (usedByRoles) {
      throw new BusinessException(ErrorCode.CONFLICT, "Menu is used by roles");
    }
    entity.setDeleted(1);
    menuMapper.updateById(entity);
  }

  List<MenuTreeVO> buildTree(List<MenuEntity> menus) {
    Map<Long, MenuTreeVO> nodeMap = new LinkedHashMap<>();
    for (MenuEntity menu : menus) {
      MenuTreeVO node = toTreeVO(menu);
      nodeMap.put(node.getId(), node);
    }
    for (MenuTreeVO node : nodeMap.values()) {
      MenuTreeVO parent = nodeMap.get(node.getParentId());
      if (parent != null) {
        parent.getChildren().add(node);
      }
    }
    Comparator<MenuTreeVO> comparator = Comparator
        .comparing(MenuTreeVO::getSortOrder, Comparator.nullsLast(Integer::compareTo))
        .thenComparing(MenuTreeVO::getId);
    nodeMap.values().forEach(node -> node.getChildren().sort(comparator));
    return nodeMap.values().stream()
        .filter(node -> node.getParentId() == null || node.getParentId() == 0 || !nodeMap.containsKey(node.getParentId()))
        .sorted(comparator)
        .toList();
  }

  private void ensureCodeAvailable(Long tenantId, String code) {
    boolean exists = menuMapper.exists(new LambdaQueryWrapper<MenuEntity>()
        .eq(MenuEntity::getTenantId, tenantId)
        .eq(MenuEntity::getCode, code)
        .eq(MenuEntity::getDeleted, 0));
    if (exists) {
      throw new BusinessException(ErrorCode.CONFLICT, "Menu code already exists");
    }
  }

  private void validateParent(Long tenantId, Long parentId, Long currentId) {
    if (parentId == null || parentId == 0) {
      return;
    }
    if (parentId.equals(currentId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Menu parent cannot be itself");
    }
    boolean parentExists = menuMapper.exists(new LambdaQueryWrapper<MenuEntity>()
        .eq(MenuEntity::getTenantId, tenantId)
        .eq(MenuEntity::getId, parentId)
        .eq(MenuEntity::getDeleted, 0));
    if (!parentExists) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Menu parent not found");
    }
  }

  private MenuEntity getExisting(Long tenantId, Long id) {
    MenuEntity entity = menuMapper.selectOne(new LambdaQueryWrapper<MenuEntity>()
        .eq(MenuEntity::getTenantId, tenantId)
        .eq(MenuEntity::getId, id)
        .eq(MenuEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Menu not found");
    }
    return entity;
  }

  private MenuTreeVO toTreeVO(MenuEntity entity) {
    MenuTreeVO vo = new MenuTreeVO();
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
    vo.setVisible(entity.getVisible());
    vo.setStatus(entity.getStatus());
    return vo;
  }
}
