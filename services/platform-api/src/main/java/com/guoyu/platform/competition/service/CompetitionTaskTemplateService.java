package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.converter.CompetitionTaskTemplateConverter;
import com.guoyu.platform.competition.dto.CompetitionTaskTemplateCreateReq;
import com.guoyu.platform.competition.dto.CompetitionTaskTemplateQueryReq;
import com.guoyu.platform.competition.dto.CompetitionTaskTemplateUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionTaskTemplateEntity;
import com.guoyu.platform.competition.enums.CompetitionTaskTemplateType;
import com.guoyu.platform.competition.mapper.CompetitionTaskTemplateMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionTaskTemplateService {

  private final CompetitionTaskTemplateMapper mapper;

  public CompetitionTaskTemplateService(CompetitionTaskTemplateMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public Page<CompetitionTaskTemplateEntity> page(
      Long tenantId, Long stageId, CompetitionTaskTemplateQueryReq req) {
    LambdaQueryWrapper<CompetitionTaskTemplateEntity> wrapper = new LambdaQueryWrapper<CompetitionTaskTemplateEntity>()
        .eq(CompetitionTaskTemplateEntity::getTenantId, tenantId)
        .eq(CompetitionTaskTemplateEntity::getStageId, stageId)
        .orderByAsc(CompetitionTaskTemplateEntity::getSortOrder)
        .orderByAsc(CompetitionTaskTemplateEntity::getId);
    if (req.getTaskType() != null && !req.getTaskType().isBlank()) {
      wrapper.eq(CompetitionTaskTemplateEntity::getTaskType, validateTaskType(req.getTaskType()));
    }
    if (req.getStatus() != null && !req.getStatus().isBlank()) {
      wrapper.eq(CompetitionTaskTemplateEntity::getStatus, req.getStatus());
    }
    Page<CompetitionTaskTemplateEntity> page = mapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);
    if (page == null) {
      log.warn("Competition task template mapper returned null page, tenantId={}, stageId={}",
          tenantId, stageId);
      return new Page<>(req.getPage(), req.getPageSize());
    }
    return page;
  }

  @Transactional
  public CompetitionTaskTemplateEntity create(
      Long tenantId, Long stageId, CompetitionTaskTemplateCreateReq req) {
    String taskType = validateTaskType(req.taskType());
    CompetitionTaskTemplateEntity entity = CompetitionTaskTemplateConverter.toEntity(req);
    entity.setTenantId(tenantId);
    entity.setStageId(stageId);
    entity.setTaskType(taskType);
    entity.setRequiresReview(req.requiresReview() == null ? 0 : req.requiresReview());
    entity.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    entity.setStatus("draft");
    entity.setDeleted(0);
    mapper.insert(entity);
    return entity;
  }

  @Transactional(readOnly = true)
  public CompetitionTaskTemplateEntity getById(Long tenantId, Long templateId) {
    return findExisting(tenantId, templateId);
  }

  @Transactional
  public CompetitionTaskTemplateEntity update(
      Long tenantId, Long templateId, CompetitionTaskTemplateUpdateReq req) {
    CompetitionTaskTemplateEntity entity = findExisting(tenantId, templateId);
    entity.setTaskType(validateTaskType(req.taskType()));
    CompetitionTaskTemplateConverter.applyUpdate(entity, req);
    mapper.updateById(entity);
    return entity;
  }

  private CompetitionTaskTemplateEntity findExisting(Long tenantId, Long templateId) {
    CompetitionTaskTemplateEntity entity = mapper.selectOne(new LambdaQueryWrapper<CompetitionTaskTemplateEntity>()
        .eq(CompetitionTaskTemplateEntity::getTenantId, tenantId)
        .eq(CompetitionTaskTemplateEntity::getId, templateId));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "任务模板不存在");
    }
    return entity;
  }

  private String validateTaskType(String taskType) {
    for (CompetitionTaskTemplateType value : CompetitionTaskTemplateType.values()) {
      if (value.getCode().equals(taskType)) {
        return taskType;
      }
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "任务模板类型不合法");
  }
}
