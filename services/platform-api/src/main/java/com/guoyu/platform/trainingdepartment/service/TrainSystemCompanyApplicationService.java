package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.trainingdepartment.dto.SystemCompanyCreateReq;
import com.guoyu.platform.trainingdepartment.dto.SystemCompanyUpdateReq;
import com.guoyu.platform.trainingdepartment.vo.SystemCompanyVO;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 培训支持保障 · 系统企业管理（sys_org.orgType=company）。
 */
@Service
public class TrainSystemCompanyApplicationService {

  private final OrgMapper orgMapper;
  private final TrainingOperatorScopeService scopeService;

  public TrainSystemCompanyApplicationService(
      OrgMapper orgMapper, TrainingOperatorScopeService scopeService) {
    this.orgMapper = orgMapper;
    this.scopeService = scopeService;
  }

  @Transactional(readOnly = true)
  public List<SystemCompanyVO> list(String keyword) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<OrgEntity> qw = new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getDeleted, 0)
        .eq(OrgEntity::getOrgType, "company")
        .orderByAsc(OrgEntity::getSortOrder)
        .orderByAsc(OrgEntity::getId);
    applyManageScope(qw);
    if (StringUtils.hasText(keyword)) {
      String kw = keyword.trim();
      qw.and(w -> w.like(OrgEntity::getName, kw).or().like(OrgEntity::getCode, kw));
    }
    List<OrgEntity> companies = orgMapper.selectList(qw);
    Map<Long, String> parentNames = loadParentNames(tenantId, companies);
    return companies.stream().map(e -> toVO(e, parentNames)).toList();
  }

  @Transactional
  public SystemCompanyVO create(SystemCompanyCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    String name = req.name().trim();
    ensureNameAvailable(tenantId, name, null);

    // 企业账号不可创建其它企业；机构建企自动挂本机构下（P15）
    if (scopeService.isEnterpriseScoped()) {
      throw new BusinessException("FORBIDDEN", "企业账号不可创建新企业");
    }

    OrgEntity entity = new OrgEntity();
    entity.setTenantId(tenantId);
    entity.setParentId(scopeService.resolveNewEnterpriseParentId());
    entity.setOrgType("company");
    entity.setCode(nextCode(tenantId));
    entity.setName(name);
    entity.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    entity.setStatus(StringUtils.hasText(req.status()) ? req.status() : "enabled");
    entity.setDeleted(0);
    orgMapper.insert(entity);
    return toVO(entity, loadParentNames(tenantId, List.of(entity)));
  }

  @Transactional
  public SystemCompanyVO update(Long id, SystemCompanyUpdateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    scopeService.assertCanManageEnterprise(id);
    OrgEntity entity = getCompany(tenantId, id);
    String name = req.name().trim();
    ensureNameAvailable(tenantId, name, id);

    entity.setName(name);
    entity.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    entity.setStatus(req.status());
    orgMapper.updateById(entity);
    return toVO(entity, loadParentNames(tenantId, List.of(entity)));
  }

  @Transactional
  public void delete(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    scopeService.assertCanManageEnterprise(id);
    if (scopeService.isEnterpriseScoped()) {
      throw new BusinessException("FORBIDDEN", "企业账号不可删除企业");
    }
    OrgEntity entity = getCompany(tenantId, id);
    boolean hasChildren = orgMapper.exists(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getParentId, id)
        .eq(OrgEntity::getDeleted, 0));
    if (hasChildren) {
      throw new BusinessException("CONFLICT", "该企业下存在子组织，无法删除");
    }
    entity.setDeleted(1);
    orgMapper.updateById(entity);
  }

  private void applyManageScope(LambdaQueryWrapper<OrgEntity> qw) {
    // 超管全量；企业仅本企；机构 = 树下属 ∪ 业务关联（需求选企另走 public-data 全量名录）
    Set<Long> managed = scopeService.resolveManagedEnterpriseIds();
    if (managed == null) {
      return;
    }
    if (managed.isEmpty()) {
      qw.eq(OrgEntity::getId, -1L);
    } else {
      qw.in(OrgEntity::getId, managed);
    }
  }

  private OrgEntity getCompany(Long tenantId, Long id) {
    OrgEntity entity = orgMapper.selectOne(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getId, id)
        .eq(OrgEntity::getOrgType, "company")
        .eq(OrgEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException("NOT_FOUND", "企业不存在");
    }
    return entity;
  }

  private void ensureNameAvailable(Long tenantId, String name, Long excludeId) {
    LambdaQueryWrapper<OrgEntity> qw = new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getDeleted, 0)
        .eq(OrgEntity::getOrgType, "company")
        .eq(OrgEntity::getName, name);
    if (excludeId != null) {
      qw.ne(OrgEntity::getId, excludeId);
    }
    if (orgMapper.exists(qw)) {
      throw new BusinessException("CONFLICT", "企业名称已存在");
    }
  }

  private String nextCode(Long tenantId) {
    String code = "ENT" + System.currentTimeMillis();
    boolean exists = orgMapper.exists(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getCode, code)
        .eq(OrgEntity::getDeleted, 0));
    if (exists) {
      code = "ENT" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }
    return code;
  }

  private Map<Long, String> loadParentNames(Long tenantId, List<OrgEntity> companies) {
    Set<Long> parentIds = companies.stream()
        .map(OrgEntity::getParentId)
        .filter(Objects::nonNull)
        .filter(id -> id > 0)
        .collect(Collectors.toSet());
    if (parentIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return orgMapper.selectList(new LambdaQueryWrapper<OrgEntity>()
            .eq(OrgEntity::getTenantId, tenantId)
            .eq(OrgEntity::getDeleted, 0)
            .in(OrgEntity::getId, parentIds))
        .stream()
        .collect(Collectors.toMap(OrgEntity::getId, OrgEntity::getName, (a, b) -> a));
  }

  private SystemCompanyVO toVO(OrgEntity e, Map<Long, String> parentNames) {
    Long parentId = e.getParentId();
    String parentName = null;
    if (parentId != null && parentId > 0) {
      parentName = parentNames.get(parentId);
    }
    return new SystemCompanyVO(
        e.getId(),
        e.getCode(),
        e.getName(),
        parentId,
        parentName,
        e.getSortOrder(),
        e.getStatus());
  }
}
