package com.guoyu.platform.system.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.dto.OrgCreateReq;
import com.guoyu.platform.system.dto.OrgUpdateReq;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.vo.OrgSimpleVO;
import com.guoyu.platform.system.vo.OrgTreeVO;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class OrgApplicationService {

  private final OrgMapper orgMapper;

  public OrgApplicationService(OrgMapper orgMapper) {
    this.orgMapper = orgMapper;
  }

  @Transactional(readOnly = true)
  public List<OrgSimpleVO> listSimple() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<OrgEntity> list = orgMapper.selectList(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getDeleted, 0)
        .eq(OrgEntity::getOrgType, "company")
        .orderByAsc(OrgEntity::getSortOrder));
    return list.stream()
        .map(o -> new OrgSimpleVO(o.getId(), o.getName(), o.getOrgType(), o.getSortOrder()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<OrgTreeVO> tree() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<OrgEntity> orgs = orgMapper.selectList(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getDeleted, 0)
        .orderByAsc(OrgEntity::getSortOrder)
        .orderByAsc(OrgEntity::getId));
    return buildTree(orgs);
  }

  @Transactional
  public OrgTreeVO create(OrgCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    validateParent(tenantId, req.parentId(), null);
    ensureCodeAvailable(tenantId, req.code());

    OrgEntity entity = new OrgEntity();
    entity.setTenantId(tenantId);
    entity.setParentId(req.parentId());
    entity.setOrgType(req.orgType());
    entity.setCode(req.code());
    entity.setName(req.name());
    entity.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    entity.setStatus(StringUtils.hasText(req.status()) ? req.status() : "enabled");
    entity.setDeleted(0);
    orgMapper.insert(entity);
    return toTreeVO(entity);
  }

  @Transactional
  public OrgTreeVO update(Long id, OrgUpdateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    OrgEntity entity = getExisting(tenantId, id);
    validateParent(tenantId, req.parentId(), id);

    entity.setParentId(req.parentId());
    entity.setOrgType(req.orgType());
    entity.setName(req.name());
    entity.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    entity.setStatus(req.status());
    orgMapper.updateById(entity);
    return toTreeVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    OrgEntity entity = getExisting(tenantId, id);
    boolean hasChildren = orgMapper.exists(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getParentId, id)
        .eq(OrgEntity::getDeleted, 0));
    if (hasChildren) {
      throw new BusinessException(ErrorCode.CONFLICT, "Org has child nodes");
    }
    entity.setDeleted(1);
    orgMapper.deleteById(entity.getId());
  }

  List<OrgTreeVO> buildTree(List<OrgEntity> orgs) {
    Map<Long, OrgTreeVO> nodeMap = new LinkedHashMap<>();
    for (OrgEntity org : orgs) {
      OrgTreeVO node = toTreeVO(org);
      nodeMap.put(node.getId(), node);
    }
    for (OrgTreeVO node : nodeMap.values()) {
      OrgTreeVO parent = nodeMap.get(node.getParentId());
      if (parent != null) {
        parent.getChildren().add(node);
      }
    }
    Comparator<OrgTreeVO> comparator = Comparator
        .comparing(OrgTreeVO::getSortOrder, Comparator.nullsLast(Integer::compareTo))
        .thenComparing(OrgTreeVO::getId);
    nodeMap.values().forEach(node -> node.getChildren().sort(comparator));
    return nodeMap.values().stream()
        .filter(node -> node.getParentId() == null || node.getParentId() == 0 || !nodeMap.containsKey(node.getParentId()))
        .sorted(comparator)
        .toList();
  }

  private void ensureCodeAvailable(Long tenantId, String code) {
    boolean exists = orgMapper.exists(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getCode, code)
        .eq(OrgEntity::getDeleted, 0));
    if (exists) {
      throw new BusinessException(ErrorCode.CONFLICT, "Org code already exists");
    }
  }

  private void validateParent(Long tenantId, Long parentId, Long currentId) {
    if (parentId == null || parentId == 0) {
      return;
    }
    if (parentId.equals(currentId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Org parent cannot be itself");
    }
    boolean parentExists = orgMapper.exists(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getId, parentId)
        .eq(OrgEntity::getDeleted, 0));
    if (!parentExists) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Org parent not found");
    }
  }

  private OrgEntity getExisting(Long tenantId, Long id) {
    OrgEntity entity = orgMapper.selectOne(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getId, id)
        .eq(OrgEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Org not found");
    }
    return entity;
  }

  private OrgTreeVO toTreeVO(OrgEntity entity) {
    OrgTreeVO vo = new OrgTreeVO();
    vo.setId(entity.getId());
    vo.setParentId(entity.getParentId());
    vo.setOrgType(entity.getOrgType());
    vo.setCode(entity.getCode());
    vo.setName(entity.getName());
    vo.setSortOrder(entity.getSortOrder());
    vo.setStatus(entity.getStatus());
    return vo;
  }
}
