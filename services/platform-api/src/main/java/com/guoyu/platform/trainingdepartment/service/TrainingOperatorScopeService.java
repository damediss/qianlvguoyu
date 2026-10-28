package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.PlatformAdminSupport;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 培训模块操作范围：对齐 yun_shang loginRole（org=培训机构 / company=企业）。
 * M1：机构可见企业 = 组织树下属 ∪ 业务关联；master 不并入企业。
 */
@Service
public class TrainingOperatorScopeService {

  private static final String ROLE_PLATFORM_ADMIN = "PLATFORM_ADMIN";
  private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
  /** 仅 company 为企业身份；master（技能大师工作室）完全排除 */
  private static final Set<String> ENTERPRISE_ORG_TYPES = Set.of("company");

  private final UserMapper userMapper;
  private final OrgMapper orgMapper;
  private final OrgEnterpriseLinkService orgEnterpriseLinkService;

  public TrainingOperatorScopeService(
      UserMapper userMapper,
      OrgMapper orgMapper,
      OrgEnterpriseLinkService orgEnterpriseLinkService) {
    this.userMapper = userMapper;
    this.orgMapper = orgMapper;
    this.orgEnterpriseLinkService = orgEnterpriseLinkService;
  }

  public record Scope(Long orgId, String orgType, String loginRole) {

    public boolean isOrgOperator() {
      return "org".equals(loginRole);
    }

    public boolean isEnterpriseViewer() {
      return "company".equals(loginRole);
    }
  }

  public Scope currentScope() {
    return scopeForUser(SecurityContext.currentUser().getUserId());
  }

  public Scope scopeForUser(Long userId) {
    UserEntity user = userMapper.selectById(userId);
    if (user == null || user.getOrgId() == null) {
      return new Scope(null, null, "org");
    }
    OrgEntity org = orgMapper.selectById(user.getOrgId());
    if (org == null) {
      return new Scope(user.getOrgId(), null, "org");
    }
    // master 不走企业/机构培训 Scope 分支（另案）
    if ("master".equals(org.getOrgType())) {
      return new Scope(user.getOrgId(), org.getOrgType(), "master");
    }
    String loginRole = ENTERPRISE_ORG_TYPES.contains(org.getOrgType()) ? "company" : "org";
    return new Scope(user.getOrgId(), org.getOrgType(), loginRole);
  }

  public void requireOrgOperator() {
    if (currentScope().isOrgOperator() || isPlatformAdmin()) {
      return;
    }
    throw new BusinessException(ErrorCode.FORBIDDEN, "仅培训机构账号可操作");
  }

  /**
   * 企业账号强制按本企业过滤；机构/超管返回请求值或 null。
   * 列表过滤请用 {@link #resolveCompanyIdScope}（机构需「下属 ∪ 关联」）。
   * 注意：需求选企建关联前勿对机构做可达性拒绝。
   */
  public Long resolveCompanyFilter(Long requestedCompanyId) {
    Scope scope = currentScope();
    if (!isPlatformAdmin() && scope.isEnterpriseViewer() && scope.orgId() != null) {
      return scope.orgId();
    }
    if (requestedCompanyId != null && requestedCompanyId != 0) {
      return requestedCompanyId;
    }
    return null;
  }

  /**
   * 列表/查询企业范围。
   *
   * <ul>
   *   <li>超管：null = 不限制；若带 requested 则单企业
   *   <li>企业：单元素集合（本企）
   *   <li>机构：树下属 ∪ 业务关联；可与 requested 求交；空集 = 无可见数据
   *   <li>master：空集
   * </ul>
   */
  public Set<Long> resolveCompanyIdScope(Long requestedCompanyId) {
    if (isPlatformAdmin()) {
      if (requestedCompanyId != null && requestedCompanyId != 0) {
        return Set.of(requestedCompanyId);
      }
      return null;
    }
    Scope scope = currentScope();
    if (scope.isEnterpriseViewer() && scope.orgId() != null) {
      if (requestedCompanyId != null
          && requestedCompanyId != 0
          && !scope.orgId().equals(requestedCompanyId)) {
        return Set.of();
      }
      return Set.of(scope.orgId());
    }
    if (scope.isOrgOperator() && scope.orgId() != null) {
      Long tenantId = SecurityContext.currentUser().getTenantId();
      Set<Long> visible =
          orgEnterpriseLinkService.listVisibleEnterpriseIds(tenantId, scope.orgId());
      if (requestedCompanyId != null && requestedCompanyId != 0) {
        if (!visible.contains(requestedCompanyId)) {
          return Set.of();
        }
        return Set.of(requestedCompanyId);
      }
      return visible.isEmpty() ? Set.of() : Collections.unmodifiableSet(visible);
    }
    return Set.of();
  }

  /** 当前机构操作者组织 ID（非机构则 null） */
  public Long currentTrainingOrgId() {
    if (isPlatformAdmin()) {
      return null;
    }
    Scope scope = currentScope();
    return scope.isOrgOperator() ? scope.orgId() : null;
  }

  /**
   * 机构账号选中企业后写入业务关联（幂等）。企业账号/超管不写。
   */
  public void ensureDemandLink(Long enterpriseId, Long sourceId) {
    if (isPlatformAdmin()) {
      return;
    }
    Scope scope = currentScope();
    if (!scope.isOrgOperator() || scope.orgId() == null || enterpriseId == null) {
      return;
    }
    Long tenantId = SecurityContext.currentUser().getTenantId();
    orgEnterpriseLinkService.upsertLink(
        tenantId,
        scope.orgId(),
        enterpriseId,
        OrgEnterpriseLinkService.SOURCE_DEMAND,
        sourceId);
  }

  public void assertClassAccessible(TrainInfoClassEntity entity) {
    if (entity == null || isPlatformAdmin()) {
      return;
    }
    Scope scope = currentScope();
    if (scope.isEnterpriseViewer()) {
      if (scope.orgId() == null
          || entity.getCompanyId() == null
          || !scope.orgId().equals(entity.getCompanyId())) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该班级");
      }
      return;
    }
    if (scope.isOrgOperator() && scope.orgId() != null) {
      assertCompanyAccessible(entity.getCompanyId());
    }
  }

  /**
   * 企业资源可达性：企业仅本企；机构仅下属 ∪ 关联；超管不限制。
   */
  public void assertCompanyAccessible(Long companyId) {
    if (isPlatformAdmin()) {
      return;
    }
    Scope scope = currentScope();
    if (scope.isEnterpriseViewer()) {
      if (scope.orgId() == null || companyId == null || !scope.orgId().equals(companyId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该企业数据");
      }
      return;
    }
    if (scope.isOrgOperator() && scope.orgId() != null) {
      if (companyId == null) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该企业数据");
      }
      Set<Long> visible =
          orgEnterpriseLinkService.listVisibleEnterpriseIds(
              SecurityContext.currentUser().getTenantId(), scope.orgId());
      if (!visible.contains(companyId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该企业数据");
      }
    }
  }

  /** 当前是否为企业视角且需强制本企业过滤（非平台管理员） */
  public boolean isEnterpriseScoped() {
    return !isPlatformAdmin() && currentScope().isEnterpriseViewer() && currentScope().orgId() != null;
  }

  /** 当前是否为机构视角（非平台管理员） */
  public boolean isOrgScoped() {
    return !isPlatformAdmin() && currentScope().isOrgOperator() && currentScope().orgId() != null;
  }

  /**
   * 企业管理/开设账号：超管任意；企业仅本企；机构为树下属 ∪ 业务关联。
   */
  public void assertCanManageEnterprise(Long enterpriseId) {
    if (enterpriseId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "企业不能为空");
    }
    if (isPlatformAdmin()) {
      return;
    }
    Scope scope = currentScope();
    if (scope.orgId() == null) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权管理该企业");
    }
    if (scope.isEnterpriseViewer()) {
      if (!scope.orgId().equals(enterpriseId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "仅可管理本企业账号");
      }
      return;
    }
    OrgEntity enterprise = orgMapper.selectById(enterpriseId);
    if (enterprise == null || !"company".equals(enterprise.getOrgType())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "企业不存在");
    }
    Set<Long> visible =
        orgEnterpriseLinkService.listVisibleEnterpriseIds(
            SecurityContext.currentUser().getTenantId(), scope.orgId());
    if (!visible.contains(enterpriseId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "仅可管理本机构下属或已关联企业");
    }
  }

  /**
   * 企业管理列表可见企业 ID；null 表示不限制（超管）；空集表示无可见。
   */
  public Set<Long> resolveManagedEnterpriseIds() {
    if (isPlatformAdmin()) {
      return null;
    }
    Scope scope = currentScope();
    if (scope.isEnterpriseViewer() && scope.orgId() != null) {
      return Set.of(scope.orgId());
    }
    if (scope.isOrgOperator() && scope.orgId() != null) {
      return orgEnterpriseLinkService.listVisibleEnterpriseIds(
          SecurityContext.currentUser().getTenantId(), scope.orgId());
    }
    return Set.of();
  }

  /** @deprecated 请用 {@link #resolveManagedEnterpriseIds()} */
  @Deprecated
  public Long resolveManagedEnterpriseFilter() {
    if (isPlatformAdmin()) {
      return null;
    }
    Scope scope = currentScope();
    if (scope.isEnterpriseViewer() && scope.orgId() != null) {
      return scope.orgId();
    }
    return null;
  }

  /** 机构建企时：机构账号自动挂到本机构下；超管/其它保持独立（parent=0） */
  public Long resolveNewEnterpriseParentId() {
    if (isPlatformAdmin()) {
      return 0L;
    }
    Scope scope = currentScope();
    if (scope.isOrgOperator() && scope.orgId() != null) {
      return scope.orgId();
    }
    return 0L;
  }

  /** 平台管理员 / 超级管理员：可跨机构查看培训相关数据 */
  public boolean isPlatformAdmin() {
    return isPlatformAdmin(SecurityContext.currentUser());
  }

  public boolean isPlatformAdmin(PlatformUserPrincipal principal) {
    if (principal == null) {
      return false;
    }
    List<String> roleCodes = principal.getRoleCodes() == null ? List.of() : principal.getRoleCodes();
    return "admin".equals(principal.getUserType())
        || PlatformAdminSupport.isSuperAdmin(principal)
        || roleCodes.stream().anyMatch(ROLE_PLATFORM_ADMIN::equalsIgnoreCase)
        || roleCodes.stream().anyMatch(ROLE_SUPER_ADMIN::equalsIgnoreCase);
  }
}
