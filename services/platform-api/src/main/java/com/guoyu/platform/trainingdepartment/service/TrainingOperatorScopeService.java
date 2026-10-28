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
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 培训模块操作范围：对齐 yun_shang loginRole（org=培训机构 / company=企业）
 */
@Service
public class TrainingOperatorScopeService {

  private static final String ROLE_PLATFORM_ADMIN = "PLATFORM_ADMIN";
  private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
  private static final Set<String> ENTERPRISE_ORG_TYPES = Set.of("company", "master");

  private final UserMapper userMapper;
  private final OrgMapper orgMapper;

  public TrainingOperatorScopeService(UserMapper userMapper, OrgMapper orgMapper) {
    this.userMapper = userMapper;
    this.orgMapper = orgMapper;
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
    String loginRole = ENTERPRISE_ORG_TYPES.contains(org.getOrgType()) ? "company" : "org";
    return new Scope(user.getOrgId(), org.getOrgType(), loginRole);
  }

  public void requireOrgOperator() {
    if (currentScope().isOrgOperator() || isPlatformAdmin()) {
      return;
    }
    throw new BusinessException(ErrorCode.FORBIDDEN, "仅培训机构账号可操作");
  }

  /** 企业账号强制按本企业过滤；机构账号使用请求中的筛选 */
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

  public void assertClassAccessible(TrainInfoClassEntity entity) {
    if (entity == null || isPlatformAdmin()) {
      return;
    }
    Scope scope = currentScope();
    if (!scope.isEnterpriseViewer() || scope.orgId() == null) {
      return;
    }
    // 企业账号：班级必须归属本企业（companyId 为空亦拒绝，避免漏标串数）
    if (entity.getCompanyId() == null || !scope.orgId().equals(entity.getCompanyId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该班级");
    }
  }

  /**
   * 企业资源（调研目标企业 / 培训信息 companyId / 考试 orgId 等）可达性校验。
   * 机构与平台管理员不限制。
   */
  public void assertCompanyAccessible(Long companyId) {
    if (isPlatformAdmin()) {
      return;
    }
    Scope scope = currentScope();
    if (!scope.isEnterpriseViewer() || scope.orgId() == null) {
      return;
    }
    if (companyId == null || !scope.orgId().equals(companyId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该企业数据");
    }
  }

  /** 当前是否为企业视角且需强制本企业过滤（非平台管理员） */
  public boolean isEnterpriseScoped() {
    return !isPlatformAdmin() && currentScope().isEnterpriseViewer() && currentScope().orgId() != null;
  }

  /**
   * 企业管理/开设账号：是否可操作该企业组织。
   * 超管任意；企业仅本企；机构仅组织树下属（parent=本机构）。业务关联表接入后可在此扩展。
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
    if (!scope.orgId().equals(enterprise.getParentId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "仅可管理本机构下属企业");
    }
  }

  /** 企业管理列表可见的企业 ID；null 表示不限制（超管） */
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

  /** 机构企业时：机构账号自动挂到本机构下；超管/其它保持独立（parent=0） */
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
