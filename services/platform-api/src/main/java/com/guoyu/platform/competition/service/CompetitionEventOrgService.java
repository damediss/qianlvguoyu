package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionEventOrgCreateReq;
import com.guoyu.platform.competition.dto.CompetitionEventOrgQueryReq;
import com.guoyu.platform.competition.dto.CompetitionEventOrgStatusUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.enums.CompetitionEventOrgStatus;
import com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionEventOrgService {

  private final CompetitionEventOrgMapper mapper;

  public CompetitionEventOrgService(CompetitionEventOrgMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public Page<CompetitionEventOrgEntity> page(Long tenantId, Long eventId, CompetitionEventOrgQueryReq req) {
    LambdaQueryWrapper<CompetitionEventOrgEntity> wrapper = new LambdaQueryWrapper<CompetitionEventOrgEntity>()
        .eq(CompetitionEventOrgEntity::getTenantId, tenantId)
        .eq(CompetitionEventOrgEntity::getEventId, eventId)
        .orderByDesc(CompetitionEventOrgEntity::getCreatedAt)
        .orderByDesc(CompetitionEventOrgEntity::getId);
    if (req.getStatus() != null && !req.getStatus().isBlank()) {
      wrapper.eq(CompetitionEventOrgEntity::getStatus, validateStatus(req.getStatus()));
    }
    if (req.getOrgId() != null) {
      wrapper.eq(CompetitionEventOrgEntity::getOrgId, req.getOrgId());
    }

    Page<CompetitionEventOrgEntity> page = mapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);
    if (page == null) {
      log.warn("Competition event organization mapper returned null page, tenantId={}, eventId={}",
          tenantId, eventId);
      return new Page<>(req.getPage(), req.getPageSize());
    }
    return page;
  }

  @Transactional
  public CompetitionEventOrgEntity create(
      Long tenantId, Long eventId, CompetitionEventOrgCreateReq req) {
    if (existsRelation(tenantId, eventId, req.orgId())) {
      throw new BusinessException(ErrorCode.CONFLICT, "赛事参与企业已存在");
    }
    validateImportPermission(req.importPermission());

    CompetitionEventOrgEntity entity = new CompetitionEventOrgEntity();
    entity.setTenantId(tenantId);
    entity.setEventId(eventId);
    entity.setOrgId(req.orgId());
    entity.setQuota(req.quota());
    entity.setStatus(CompetitionEventOrgStatus.INVITED.getCode());
    entity.setImportPermission(req.importPermission() == null ? 0 : req.importPermission());
    entity.setDeleted(0);
    mapper.insert(entity);
    return entity;
  }

  @Transactional
  public CompetitionEventOrgEntity updateStatus(
      Long tenantId, Long eventId, Long orgId, CompetitionEventOrgStatusUpdateReq req) {
    CompetitionEventOrgEntity entity = findExisting(tenantId, eventId, orgId);
    entity.setStatus(validateStatus(req.status()));
    mapper.updateById(entity);
    return entity;
  }

  private boolean existsRelation(Long tenantId, Long eventId, Long orgId) {
    CompetitionEventOrgEntity existing = mapper.selectOne(new LambdaQueryWrapper<CompetitionEventOrgEntity>()
        .eq(CompetitionEventOrgEntity::getTenantId, tenantId)
        .eq(CompetitionEventOrgEntity::getEventId, eventId)
        .eq(CompetitionEventOrgEntity::getOrgId, orgId));
    return existing != null;
  }

  private CompetitionEventOrgEntity findExisting(Long tenantId, Long eventId, Long orgId) {
    CompetitionEventOrgEntity entity = mapper.selectOne(new LambdaQueryWrapper<CompetitionEventOrgEntity>()
        .eq(CompetitionEventOrgEntity::getTenantId, tenantId)
        .eq(CompetitionEventOrgEntity::getEventId, eventId)
        .eq(CompetitionEventOrgEntity::getOrgId, orgId));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事参与企业关系不存在");
    }
    return entity;
  }

  private String validateStatus(String status) {
    for (CompetitionEventOrgStatus value : CompetitionEventOrgStatus.values()) {
      if (value.getCode().equals(status)) {
        return status;
      }
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "参与企业状态不合法");
  }

  private void validateImportPermission(Integer value) {
    if (value != null && value != 0 && value != 1) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "名单导入权限必须为0或1");
    }
  }
}
