package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 培训模块操作范围：对齐 yun_shang loginRole（org=培训机构 / company=企业）
 */
@Service
public class TrainingOperatorScopeService {

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
    if (!currentScope().isOrgOperator()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "仅培训机构账号可操作");
    }
  }

  /** 企业账号强制按本企业过滤；机构账号使用请求中的筛选 */
  public Long resolveCompanyFilter(Long requestedCompanyId) {
    Scope scope = currentScope();
    if (scope.isEnterpriseViewer() && scope.orgId() != null) {
      return scope.orgId();
    }
    if (requestedCompanyId != null && requestedCompanyId != 0) {
      return requestedCompanyId;
    }
    return null;
  }

  public void assertClassAccessible(TrainInfoClassEntity entity) {
    if (entity == null) {
      return;
    }
    Scope scope = currentScope();
    if (scope.isEnterpriseViewer() && scope.orgId() != null
        && entity.getCompanyId() != null
        && !scope.orgId().equals(entity.getCompanyId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该班级");
    }
  }
}
