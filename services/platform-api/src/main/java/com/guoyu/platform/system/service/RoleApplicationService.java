package com.guoyu.platform.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.dto.RoleCreateReq;
import com.guoyu.platform.system.dto.RolePermissionBindReq;
import com.guoyu.platform.system.dto.RoleQueryReq;
import com.guoyu.platform.system.dto.RoleUpdateReq;
import com.guoyu.platform.system.entity.MenuEntity;
import com.guoyu.platform.system.entity.PermissionEntity;
import com.guoyu.platform.system.entity.RoleEntity;
import com.guoyu.platform.system.entity.RoleMenuEntity;
import com.guoyu.platform.system.entity.RolePermissionEntity;
import com.guoyu.platform.system.entity.UserRoleEntity;
import com.guoyu.platform.system.mapper.MenuMapper;
import com.guoyu.platform.system.mapper.PermissionMapper;
import com.guoyu.platform.system.mapper.RoleMapper;
import com.guoyu.platform.system.mapper.RoleMenuMapper;
import com.guoyu.platform.system.mapper.RoleMenuRelationMapper;
import com.guoyu.platform.system.mapper.RolePermissionRelationMapper;
import com.guoyu.platform.system.mapper.UserRoleRelationMapper;
import com.guoyu.platform.system.vo.RoleVO;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RoleApplicationService {

  private final RoleMapper roleMapper;
  private final MenuMapper menuMapper;
  private final PermissionMapper permissionMapper;
  private final RoleMenuMapper roleMenuMapper;
  private final RoleMenuRelationMapper roleMenuRelationMapper;
  private final RolePermissionRelationMapper rolePermissionRelationMapper;
  private final UserRoleRelationMapper userRoleRelationMapper;

  public RoleApplicationService(
      RoleMapper roleMapper,
      MenuMapper menuMapper,
      PermissionMapper permissionMapper,
      RoleMenuMapper roleMenuMapper,
      RoleMenuRelationMapper roleMenuRelationMapper,
      RolePermissionRelationMapper rolePermissionRelationMapper,
      UserRoleRelationMapper userRoleRelationMapper
  ) {
    this.roleMapper = roleMapper;
    this.menuMapper = menuMapper;
    this.permissionMapper = permissionMapper;
    this.roleMenuMapper = roleMenuMapper;
    this.roleMenuRelationMapper = roleMenuRelationMapper;
    this.rolePermissionRelationMapper = rolePermissionRelationMapper;
    this.userRoleRelationMapper = userRoleRelationMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<RoleVO> page(RoleQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<RoleEntity>()
        .eq(RoleEntity::getTenantId, tenantId)
        .eq(RoleEntity::getDeleted, 0)
        .orderByDesc(RoleEntity::getCreatedAt);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(item -> item
          .like(RoleEntity::getCode, req.getKeyword())
          .or()
          .like(RoleEntity::getName, req.getKeyword()));
    }
    if (StringUtils.hasText(req.getRoleType())) {
      wrapper.eq(RoleEntity::getRoleType, req.getRoleType());
    }
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(RoleEntity::getStatus, req.getStatus());
    }
    Page<RoleEntity> page = roleMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(
        page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(),
        page.getSize(),
        page.getTotal()
    );
  }

  @Transactional(readOnly = true)
  public RoleVO detail(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    return toVO(getExisting(tenantId, id));
  }

  @Transactional
  public RoleVO create(RoleCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    ensureCodeAvailable(tenantId, req.code());

    RoleEntity entity = new RoleEntity();
    entity.setTenantId(tenantId);
    entity.setCode(req.code());
    entity.setName(req.name());
    entity.setRoleType(req.roleType());
    entity.setStatus(StringUtils.hasText(req.status()) ? req.status() : "enabled");
    entity.setRemark(req.remark());
    entity.setDeleted(0);
    roleMapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public RoleVO update(Long id, RoleUpdateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    RoleEntity entity = getExisting(tenantId, id);
    entity.setName(req.name());
    entity.setRoleType(req.roleType());
    entity.setStatus(req.status());
    entity.setRemark(req.remark());
    roleMapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public RoleVO bindPermissions(Long id, RolePermissionBindReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    RoleEntity entity = getExisting(tenantId, id);
    validateMenus(tenantId, req.menuIds());
    validatePermissions(tenantId, req.permissionIds());

    roleMenuRelationMapper.physicalDeleteByRole(tenantId, id);
    for (Long menuId : new HashSet<>(req.menuIds())) {
      RoleMenuEntity relation = new RoleMenuEntity();
      relation.setTenantId(tenantId);
      relation.setRoleId(id);
      relation.setMenuId(menuId);
      relation.setDeleted(0);
      roleMenuRelationMapper.insert(relation);
    }

    rolePermissionRelationMapper.physicalDeleteByRole(tenantId, id);
    for (Long permissionId : new HashSet<>(req.permissionIds())) {
      RolePermissionEntity relation = new RolePermissionEntity();
      relation.setTenantId(tenantId);
      relation.setRoleId(id);
      relation.setPermissionId(permissionId);
      relation.setDeleted(0);
      rolePermissionRelationMapper.insert(relation);
    }
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    RoleEntity entity = getExisting(tenantId, id);
    boolean usedByUsers = userRoleRelationMapper.exists(new LambdaQueryWrapper<UserRoleEntity>()
        .eq(UserRoleEntity::getTenantId, tenantId)
        .eq(UserRoleEntity::getRoleId, id)
        .eq(UserRoleEntity::getDeleted, 0));
    if (usedByUsers) {
      throw new BusinessException(ErrorCode.CONFLICT, "Role is used by users");
    }
    entity.setDeleted(1);
    roleMapper.updateById(entity);
    roleMenuRelationMapper.physicalDeleteByRole(tenantId, id);
    rolePermissionRelationMapper.physicalDeleteByRole(tenantId, id);
  }

  private void ensureCodeAvailable(Long tenantId, String code) {
    boolean exists = roleMapper.exists(new LambdaQueryWrapper<RoleEntity>()
        .eq(RoleEntity::getTenantId, tenantId)
        .eq(RoleEntity::getCode, code)
        .eq(RoleEntity::getDeleted, 0));
    if (exists) {
      throw new BusinessException(ErrorCode.CONFLICT, "Role code already exists");
    }
  }

  private void validateMenus(Long tenantId, List<Long> menuIds) {
    if (menuIds == null || menuIds.isEmpty()) {
      return;
    }
    Set<Long> distinctIds = new HashSet<>(menuIds);
    Long count = menuMapper.selectCount(new LambdaQueryWrapper<MenuEntity>()
        .eq(MenuEntity::getTenantId, tenantId)
        .in(MenuEntity::getId, distinctIds)
        .eq(MenuEntity::getDeleted, 0));
    if (count != distinctIds.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Menu not found");
    }
  }

  private void validatePermissions(Long tenantId, List<Long> permissionIds) {
    if (permissionIds == null || permissionIds.isEmpty()) {
      return;
    }
    Set<Long> distinctIds = new HashSet<>(permissionIds);
    Long count = permissionMapper.selectCount(new LambdaQueryWrapper<PermissionEntity>()
        .eq(PermissionEntity::getTenantId, tenantId)
        .in(PermissionEntity::getId, distinctIds)
        .eq(PermissionEntity::getDeleted, 0));
    if (count != distinctIds.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Permission not found");
    }
  }

  private RoleEntity getExisting(Long tenantId, Long id) {
    RoleEntity entity = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
        .eq(RoleEntity::getTenantId, tenantId)
        .eq(RoleEntity::getId, id)
        .eq(RoleEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Role not found");
    }
    return entity;
  }

  private RoleVO toVO(RoleEntity entity) {
    return new RoleVO(
        entity.getId(),
        entity.getTenantId(),
        entity.getCode(),
        entity.getName(),
        entity.getRoleType(),
        entity.getStatus(),
        entity.getRemark(),
        roleMenuMapper.selectMenuIdsByRole(entity.getTenantId(), entity.getId()),
        roleMenuMapper.selectPermissionIdsByRole(entity.getTenantId(), entity.getId()),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
