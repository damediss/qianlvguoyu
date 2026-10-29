package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainOrgEnterpriseLinkEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainOrgEnterpriseLinkMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 机构–企业业务关联：需求选中企业后永久可查看（不改 parent）。
 */
@Service
public class OrgEnterpriseLinkService {

  public static final String SOURCE_DEMAND = "demand";
  public static final String STATUS_ACTIVE = "active";

  private final TrainOrgEnterpriseLinkMapper linkMapper;
  private final OrgMapper orgMapper;

  public OrgEnterpriseLinkService(TrainOrgEnterpriseLinkMapper linkMapper, OrgMapper orgMapper) {
    this.linkMapper = linkMapper;
    this.orgMapper = orgMapper;
  }

  /**
   * 组织树全部下属企业（含多级；org_type=company，不含 master）。
   * 中间非 company 节点也会继续下钻，以覆盖「机构→中间节点→企业」挂法。
   */
  @Transactional(readOnly = true)
  public Set<Long> listTreeChildEnterpriseIds(Long tenantId, Long orgId) {
    if (tenantId == null || orgId == null) {
      return Set.of();
    }
    Set<Long> enterprises = new LinkedHashSet<>();
    Set<Long> visited = new LinkedHashSet<>();
    Set<Long> frontier = new LinkedHashSet<>();
    frontier.add(orgId);
    int guard = 0;
    while (!frontier.isEmpty() && guard++ < 32) {
      List<OrgEntity> children =
          orgMapper.selectList(
              new LambdaQueryWrapper<OrgEntity>()
                  .eq(OrgEntity::getTenantId, tenantId)
                  .in(OrgEntity::getParentId, frontier)
                  .eq(OrgEntity::getDeleted, 0));
      frontier = new LinkedHashSet<>();
      for (OrgEntity child : children) {
        if (child.getId() == null || !visited.add(child.getId())) {
          continue;
        }
        if ("company".equals(child.getOrgType())) {
          enterprises.add(child.getId());
        }
        // master 不纳入培训企业 Scope，仍下钻以防其下误挂 company（极少见）
        frontier.add(child.getId());
      }
    }
    return enterprises;
  }

  /** 业务关联表中 active 的企业 */
  @Transactional(readOnly = true)
  public Set<Long> listLinkedEnterpriseIds(Long tenantId, Long orgId) {
    if (tenantId == null || orgId == null) {
      return Set.of();
    }
    List<TrainOrgEnterpriseLinkEntity> links =
        linkMapper.selectList(
            new LambdaQueryWrapper<TrainOrgEnterpriseLinkEntity>()
                .eq(TrainOrgEnterpriseLinkEntity::getTenantId, tenantId)
                .eq(TrainOrgEnterpriseLinkEntity::getOrgId, orgId)
                .eq(TrainOrgEnterpriseLinkEntity::getStatus, STATUS_ACTIVE)
                .eq(TrainOrgEnterpriseLinkEntity::getDeleted, 0));
    return links.stream()
        .map(TrainOrgEnterpriseLinkEntity::getEnterpriseId)
        .filter(id -> id != null)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /** 机构可见企业 = 树下属 ∪ 业务关联 */
  @Transactional(readOnly = true)
  public Set<Long> listVisibleEnterpriseIds(Long tenantId, Long orgId) {
    Set<Long> ids = new LinkedHashSet<>();
    ids.addAll(listTreeChildEnterpriseIds(tenantId, orgId));
    ids.addAll(listLinkedEnterpriseIds(tenantId, orgId));
    return Collections.unmodifiableSet(ids);
  }

  /**
   * 幂等 upsert：机构选中企业后建立永久关联。仅当双方组织合法时写入。
   *
   * @return true 若新建或已存在 active 关联
   */
  @Transactional
  public boolean upsertLink(
      Long tenantId, Long orgId, Long enterpriseId, String source, Long sourceId) {
    if (tenantId == null || orgId == null || enterpriseId == null || enterpriseId == 0) {
      return false;
    }
    if (orgId.equals(enterpriseId)) {
      return false;
    }
    OrgEntity org = orgMapper.selectById(orgId);
    OrgEntity enterprise = orgMapper.selectById(enterpriseId);
    if (org == null
        || (org.getDeleted() != null && org.getDeleted() == 1)
        || "company".equals(org.getOrgType())
        || "master".equals(org.getOrgType())) {
      return false;
    }
    if (enterprise == null
        || (enterprise.getDeleted() != null && enterprise.getDeleted() == 1)
        || !"company".equals(enterprise.getOrgType())) {
      return false;
    }

    TrainOrgEnterpriseLinkEntity existing =
        linkMapper.selectOne(
            new LambdaQueryWrapper<TrainOrgEnterpriseLinkEntity>()
                .eq(TrainOrgEnterpriseLinkEntity::getTenantId, tenantId)
                .eq(TrainOrgEnterpriseLinkEntity::getOrgId, orgId)
                .eq(TrainOrgEnterpriseLinkEntity::getEnterpriseId, enterpriseId)
                .eq(TrainOrgEnterpriseLinkEntity::getDeleted, 0)
                .last("LIMIT 1"));
    Long userId = SecurityContext.currentUser().getUserId();
    LocalDateTime now = LocalDateTime.now();
    if (existing != null) {
      boolean dirty = false;
      if (!STATUS_ACTIVE.equals(existing.getStatus())) {
        existing.setStatus(STATUS_ACTIVE);
        dirty = true;
      }
      if (sourceId != null && existing.getSourceId() == null) {
        existing.setSourceId(sourceId);
        dirty = true;
      }
      if (StringUtils.hasText(source) && !StringUtils.hasText(existing.getSource())) {
        existing.setSource(source);
        dirty = true;
      }
      if (dirty) {
        existing.setUpdatedAt(now);
        existing.setUpdatedBy(userId);
        linkMapper.updateById(existing);
      }
      return true;
    }

    TrainOrgEnterpriseLinkEntity entity = new TrainOrgEnterpriseLinkEntity();
    entity.setId(IdWorker.getId());
    entity.setTenantId(tenantId);
    entity.setOrgId(orgId);
    entity.setEnterpriseId(enterpriseId);
    entity.setSource(StringUtils.hasText(source) ? source : SOURCE_DEMAND);
    entity.setSourceId(sourceId);
    entity.setStatus(STATUS_ACTIVE);
    entity.setDeleted(0);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    entity.setCreatedBy(userId);
    entity.setUpdatedBy(userId);
    linkMapper.insert(entity);
    return true;
  }
}
