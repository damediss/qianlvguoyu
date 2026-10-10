package com.guoyu.platform.system.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.dto.ResetPasswordReq;
import com.guoyu.platform.system.dto.UserCreateReq;
import com.guoyu.platform.system.dto.UserQueryReq;
import com.guoyu.platform.system.dto.UserUpdateReq;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.PostEntity;
import com.guoyu.platform.system.entity.RoleEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.entity.UserRoleEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.PostMapper;
import com.guoyu.platform.system.mapper.RoleMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.system.mapper.UserRoleMapper;
import com.guoyu.platform.system.mapper.UserRoleRelationMapper;
import com.guoyu.platform.system.vo.UserVO;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class UserApplicationService {

  private final UserMapper userMapper;
  private final OrgMapper orgMapper;
  private final PostMapper postMapper;
  private final RoleMapper roleMapper;
  private final UserRoleMapper userRoleMapper;
  private final UserRoleRelationMapper userRoleRelationMapper;
  private final PasswordEncoder passwordEncoder;

  public UserApplicationService(
      UserMapper userMapper,
      OrgMapper orgMapper,
      PostMapper postMapper,
      RoleMapper roleMapper,
      UserRoleMapper userRoleMapper,
      UserRoleRelationMapper userRoleRelationMapper,
      PasswordEncoder passwordEncoder
  ) {
    this.userMapper = userMapper;
    this.orgMapper = orgMapper;
    this.postMapper = postMapper;
    this.roleMapper = roleMapper;
    this.userRoleMapper = userRoleMapper;
    this.userRoleRelationMapper = userRoleRelationMapper;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(readOnly = true)
  public PageResponse<UserVO> page(UserQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, tenantId)
        .eq(UserEntity::getDeleted, 0)
        .orderByDesc(UserEntity::getCreatedAt);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(item -> item
          .like(UserEntity::getUsername, req.getKeyword())
          .or()
          .like(UserEntity::getDisplayName, req.getKeyword())
          .or()
          .like(UserEntity::getPhone, req.getKeyword()));
    }
    if (req.getOrgId() != null) {
      wrapper.eq(UserEntity::getOrgId, req.getOrgId());
    }
    if (StringUtils.hasText(req.getUserType())) {
      wrapper.eq(UserEntity::getUserType, req.getUserType());
    }
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(UserEntity::getStatus, req.getStatus());
    }
    Page<UserEntity> page = userMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(
        page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(),
        page.getSize(),
        page.getTotal()
    );
  }

  @Transactional(readOnly = true)
  public UserVO detail(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    return toVO(getExisting(tenantId, id));
  }

  @Transactional
  public UserVO create(UserCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    return createForTenant(tenantId, req);
  }

  /**
   * Creates a user for an explicitly supplied tenant. This is intended for audited background
   * imports, where SecurityContext is intentionally unavailable outside the HTTP thread.
   */
  @Transactional
  public UserVO createForTenant(Long tenantId, UserCreateReq req) {
    validateOrg(tenantId, req.orgId());
    validatePost(tenantId, req.postId());
    validateRoles(tenantId, req.roleIds());
    ensureUsernameAvailable(tenantId, req.username());

    UserEntity entity = new UserEntity();
    entity.setTenantId(tenantId);
    entity.setOrgId(req.orgId());
    entity.setPostId(req.postId());
    entity.setUsername(req.username());
    entity.setPasswordHash(passwordEncoder.encode(req.password()));
    entity.setDisplayName(req.displayName());
    entity.setUserType(req.userType());
    entity.setPhone(req.phone());
    entity.setEmail(req.email());
    entity.setStatus(StringUtils.hasText(req.status()) ? req.status() : "enabled");
    entity.setDeleted(0);
    userMapper.insert(entity);
    replaceRoles(tenantId, entity.getId(), req.roleIds());
    return toVO(entity);
  }

  @Transactional
  public UserVO update(Long id, UserUpdateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    UserEntity entity = getExisting(tenantId, id);
    validateOrg(tenantId, req.orgId());
    validatePost(tenantId, req.postId());
    validateRoles(tenantId, req.roleIds());

    entity.setOrgId(req.orgId());
    entity.setPostId(req.postId());
    entity.setDisplayName(req.displayName());
    entity.setUserType(req.userType());
    entity.setPhone(req.phone());
    entity.setEmail(req.email());
    entity.setStatus(req.status());
    userMapper.updateById(entity);
    replaceRoles(tenantId, entity.getId(), req.roleIds());
    return toVO(entity);
  }

  @Transactional
  public void resetPassword(Long id, ResetPasswordReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    UserEntity entity = getExisting(tenantId, id);
    entity.setPasswordHash(passwordEncoder.encode(req.newPassword()));
    userMapper.updateById(entity);
  }

  @Transactional
  public void delete(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    UserEntity entity = getExisting(tenantId, id);
    entity.setDeleted(1);
    userMapper.deleteById(entity.getId());
    userRoleRelationMapper.physicalDeleteByUser(tenantId, id);
  }

  private void ensureUsernameAvailable(Long tenantId, String username) {
    boolean exists = userMapper.exists(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, tenantId)
        .eq(UserEntity::getUsername, username)
        .eq(UserEntity::getDeleted, 0));
    if (exists) {
      throw new BusinessException(ErrorCode.CONFLICT, "Username already exists");
    }
  }

  private void validateOrg(Long tenantId, Long orgId) {
    if (orgId == null) {
      return;
    }
    boolean exists = orgMapper.exists(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getId, orgId)
        .eq(OrgEntity::getDeleted, 0));
    if (!exists) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Org not found");
    }
  }

  private void validatePost(Long tenantId, Long postId) {
    if (postId == null) {
      return;
    }
    boolean exists = postMapper.exists(new LambdaQueryWrapper<PostEntity>()
        .eq(PostEntity::getTenantId, tenantId)
        .eq(PostEntity::getId, postId)
        .eq(PostEntity::getDeleted, 0));
    if (!exists) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Post not found");
    }
  }

  private void validateRoles(Long tenantId, List<Long> roleIds) {
    if (roleIds == null || roleIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "At least one role is required");
    }
    Set<Long> distinctRoleIds = new HashSet<>(roleIds);
    Long count = roleMapper.selectCount(new LambdaQueryWrapper<RoleEntity>()
        .eq(RoleEntity::getTenantId, tenantId)
        .in(RoleEntity::getId, distinctRoleIds)
        .eq(RoleEntity::getStatus, "enabled")
        .eq(RoleEntity::getDeleted, 0));
    if (count != distinctRoleIds.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Role not found");
    }
  }

  private void replaceRoles(Long tenantId, Long userId, List<Long> roleIds) {
    userRoleRelationMapper.physicalDeleteByUser(tenantId, userId);
    for (Long roleId : new HashSet<>(roleIds)) {
      UserRoleEntity relation = new UserRoleEntity();
      relation.setTenantId(tenantId);
      relation.setUserId(userId);
      relation.setRoleId(roleId);
      relation.setDeleted(0);
      userRoleRelationMapper.insert(relation);
    }
  }

  private UserEntity getExisting(Long tenantId, Long id) {
    UserEntity entity = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, tenantId)
        .eq(UserEntity::getId, id)
        .eq(UserEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "User not found");
    }
    return entity;
  }

  private UserVO toVO(UserEntity entity) {
    List<Long> roleIds = userRoleMapper.selectRoleIds(entity.getTenantId(), entity.getId());
    List<String> roleCodes = userRoleMapper.selectRoleCodes(entity.getTenantId(), entity.getId());
    String postName = null;
    if (entity.getPostId() != null) {
      PostEntity post = postMapper.selectById(entity.getPostId());
      if (post != null && post.getDeleted() == 0) {
        postName = post.getName();
      }
    }
    return new UserVO(
        entity.getId(),
        entity.getTenantId(),
        entity.getOrgId(),
        entity.getPostId(),
        postName,
        entity.getUsername(),
        entity.getDisplayName(),
        entity.getUserType(),
        entity.getPhone(),
        entity.getEmail(),
        entity.getStatus(),
        entity.getLastLoginAt(),
        roleIds,
        roleCodes,
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
