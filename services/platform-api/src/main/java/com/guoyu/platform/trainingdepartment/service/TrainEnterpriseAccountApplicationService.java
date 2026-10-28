package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.dto.ResetPasswordReq;
import com.guoyu.platform.system.dto.UserCreateReq;
import com.guoyu.platform.system.dto.UserQueryReq;
import com.guoyu.platform.system.dto.UserUpdateReq;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.RoleEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.RoleMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.system.service.UserApplicationService;
import com.guoyu.platform.system.vo.UserVO;
import com.guoyu.platform.trainingdepartment.dto.EnterpriseAccountCreateReq;
import com.guoyu.platform.trainingdepartment.dto.EnterpriseAccountUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.EnterpriseAccountRoleVO;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 培训 · 企业管理 · 开设账号（强制 org_id=企业，不走系统用户管理越权路径）。
 */
@Service
public class TrainEnterpriseAccountApplicationService {

  public static final String DEFAULT_ENTERPRISE_ROLE_CODE = "TRAIN_ENTERPRISE";
  private static final Set<String> BLOCKED_ROLE_CODES = Set.of(
      "PLATFORM_ADMIN", "SUPER_ADMIN");

  private final TrainingOperatorScopeService scopeService;
  private final EnterpriseFeatureConfigService featureConfigService;
  private final OrgMapper orgMapper;
  private final UserMapper userMapper;
  private final RoleMapper roleMapper;
  private final UserApplicationService userApplicationService;

  public TrainEnterpriseAccountApplicationService(
      TrainingOperatorScopeService scopeService,
      EnterpriseFeatureConfigService featureConfigService,
      OrgMapper orgMapper,
      UserMapper userMapper,
      RoleMapper roleMapper,
      UserApplicationService userApplicationService) {
    this.scopeService = scopeService;
    this.featureConfigService = featureConfigService;
    this.orgMapper = orgMapper;
    this.userMapper = userMapper;
    this.roleMapper = roleMapper;
    this.userApplicationService = userApplicationService;
  }

  @Transactional(readOnly = true)
  public PageResponse<UserVO> pageAccounts(Long enterpriseId, int page, int pageSize, String keyword) {
    scopeService.assertCanManageEnterprise(enterpriseId);
    ensureCompany(enterpriseId);
    UserQueryReq req = new UserQueryReq();
    req.setPage(page <= 0 ? 1 : page);
    req.setPageSize(pageSize <= 0 ? 20 : Math.min(pageSize, 100));
    req.setOrgId(enterpriseId);
    if (StringUtils.hasText(keyword)) {
      req.setKeyword(keyword.trim());
    }
    return userApplicationService.page(req);
  }

  @Transactional
  public UserVO createAccount(Long enterpriseId, EnterpriseAccountCreateReq req) {
    scopeService.assertCanManageEnterprise(enterpriseId);
    ensureCompany(enterpriseId);
    List<Long> roleIds = resolveRoleIds(enterpriseId, req.roleIds());
    UserCreateReq createReq = new UserCreateReq(
        enterpriseId,
        null,
        req.username().trim(),
        req.password(),
        req.displayName().trim(),
        "normal",
        StringUtils.hasText(req.phone()) ? req.phone().trim() : null,
        StringUtils.hasText(req.email()) ? req.email().trim() : null,
        StringUtils.hasText(req.status()) ? req.status() : "enabled",
        roleIds
    );
    return userApplicationService.create(createReq);
  }

  @Transactional
  public UserVO updateAccount(Long enterpriseId, Long userId, EnterpriseAccountUpdateReq req) {
    scopeService.assertCanManageEnterprise(enterpriseId);
    ensureCompany(enterpriseId);
    UserEntity user = requireEnterpriseUser(enterpriseId, userId);
    String status = req.status().trim();
    if (!"enabled".equals(status) && !"disabled".equals(status) && !"locked".equals(status)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "状态仅支持 enabled / disabled / locked");
    }
    List<Long> roleIds = resolveRoleIds(enterpriseId, req.roleIds());
    UserUpdateReq updateReq = new UserUpdateReq(
        enterpriseId,
        user.getPostId(),
        req.displayName().trim(),
        StringUtils.hasText(user.getUserType()) ? user.getUserType() : "normal",
        StringUtils.hasText(req.phone()) ? req.phone().trim() : null,
        StringUtils.hasText(req.email()) ? req.email().trim() : null,
        status,
        roleIds
    );
    return userApplicationService.update(userId, updateReq);
  }

  @Transactional
  public UserVO setAccountStatus(Long enterpriseId, Long userId, String status) {
    scopeService.assertCanManageEnterprise(enterpriseId);
    ensureCompany(enterpriseId);
    UserEntity user = requireEnterpriseUser(enterpriseId, userId);
    String next = status == null ? "" : status.trim();
    if (!"enabled".equals(next) && !"disabled".equals(next)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "状态仅支持启用或停用");
    }
    UserVO detail = userApplicationService.detail(userId);
    List<Long> roleIds = detail.roleIds() == null || detail.roleIds().isEmpty()
        ? resolveRoleIds(enterpriseId, null)
        : List.copyOf(detail.roleIds());
    UserUpdateReq updateReq = new UserUpdateReq(
        enterpriseId,
        user.getPostId(),
        detail.displayName(),
        StringUtils.hasText(user.getUserType()) ? user.getUserType() : "normal",
        detail.phone(),
        detail.email(),
        next,
        roleIds
    );
    return userApplicationService.update(userId, updateReq);
  }

  @Transactional
  public void resetPassword(Long enterpriseId, Long userId, ResetPasswordReq req) {
    scopeService.assertCanManageEnterprise(enterpriseId);
    ensureCompany(enterpriseId);
    requireEnterpriseUser(enterpriseId, userId);
    userApplicationService.resetPassword(userId, req);
  }

  private UserEntity requireEnterpriseUser(Long enterpriseId, Long userId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, tenantId)
        .eq(UserEntity::getId, userId)
        .eq(UserEntity::getOrgId, enterpriseId)
        .eq(UserEntity::getDeleted, 0));
    if (user == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "账号不存在或不属于该企业");
    }
    return user;
  }

  @Transactional(readOnly = true)
  public List<EnterpriseAccountRoleVO> listAssignableRoles(Long enterpriseId) {
    scopeService.assertCanManageEnterprise(enterpriseId);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<RoleEntity> roles = roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>()
        .eq(RoleEntity::getTenantId, tenantId)
        .eq(RoleEntity::getDeleted, 0)
        .eq(RoleEntity::getStatus, "enabled")
        .orderByAsc(RoleEntity::getId));
    List<Long> candidateIds = roles.stream()
        .filter(r -> r.getCode() == null
            || !BLOCKED_ROLE_CODES.contains(r.getCode().toUpperCase(Locale.ROOT)))
        .map(RoleEntity::getId)
        .toList();
    Set<Long> allowed = new HashSet<>(featureConfigService.filterAssignableRoleIds(enterpriseId, candidateIds));
    return roles.stream()
        .filter(r -> allowed.contains(r.getId()))
        .map(r -> new EnterpriseAccountRoleVO(
            r.getId(),
            r.getCode(),
            r.getName(),
            DEFAULT_ENTERPRISE_ROLE_CODE.equalsIgnoreCase(r.getCode())))
        .toList();
  }

  private List<Long> resolveRoleIds(Long enterpriseId, List<Long> requested) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    if (requested != null && !requested.isEmpty()) {
      Set<Long> distinct = new HashSet<>(requested);
      List<RoleEntity> roles = roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>()
          .eq(RoleEntity::getTenantId, tenantId)
          .in(RoleEntity::getId, distinct)
          .eq(RoleEntity::getDeleted, 0)
          .eq(RoleEntity::getStatus, "enabled"));
      if (roles.size() != distinct.size()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "角色不存在或已停用");
      }
      for (RoleEntity role : roles) {
        if (role.getCode() != null
            && BLOCKED_ROLE_CODES.contains(role.getCode().toUpperCase(Locale.ROOT))) {
          throw new BusinessException(ErrorCode.FORBIDDEN, "不可为企业账号分配平台管理员角色");
        }
      }
      featureConfigService.assertRolesAssignable(enterpriseId, List.copyOf(distinct));
      return List.copyOf(distinct);
    }
    RoleEntity defaultRole = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
        .eq(RoleEntity::getTenantId, tenantId)
        .eq(RoleEntity::getCode, DEFAULT_ENTERPRISE_ROLE_CODE)
        .eq(RoleEntity::getDeleted, 0)
        .eq(RoleEntity::getStatus, "enabled")
        .last("LIMIT 1"));
    if (defaultRole == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "未配置默认企业角色 TRAIN_ENTERPRISE，请选择角色或联系管理员");
    }
    featureConfigService.assertRolesAssignable(enterpriseId, List.of(defaultRole.getId()));
    return List.of(defaultRole.getId());
  }

  private OrgEntity ensureCompany(Long enterpriseId) {
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
}
