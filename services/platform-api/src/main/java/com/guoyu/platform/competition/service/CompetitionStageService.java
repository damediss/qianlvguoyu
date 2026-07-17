package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.converter.CompetitionStageConverter;
import com.guoyu.platform.competition.dto.CompetitionStageCreateReq;
import com.guoyu.platform.competition.dto.CompetitionStageQueryReq;
import com.guoyu.platform.competition.dto.CompetitionStageUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionStageService {

  private final CompetitionStageMapper mapper;

  public CompetitionStageService(CompetitionStageMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public Page<CompetitionStageEntity> page(Long tenantId, Long eventId, CompetitionStageQueryReq req) {
    LambdaQueryWrapper<CompetitionStageEntity> wrapper = new LambdaQueryWrapper<CompetitionStageEntity>()
        .eq(CompetitionStageEntity::getTenantId, tenantId)
        .eq(CompetitionStageEntity::getEventId, eventId)
        .orderByAsc(CompetitionStageEntity::getSortOrder)
        .orderByAsc(CompetitionStageEntity::getId);
    if (req.getStatus() != null && !req.getStatus().isBlank()) {
      wrapper.eq(CompetitionStageEntity::getStatus, req.getStatus());
    }
    Page<CompetitionStageEntity> page = mapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);
    if (page == null) {
      log.warn("Competition stage mapper returned null page, tenantId={}, eventId={}", tenantId, eventId);
      return new Page<>(req.getPage(), req.getPageSize());
    }
    return page;
  }

  @Transactional
  public CompetitionStageEntity create(Long tenantId, Long eventId, CompetitionStageCreateReq req) {
    validateDateRange(req.startAt(), req.endAt());
    CompetitionStageEntity entity = CompetitionStageConverter.toEntity(req);
    entity.setTenantId(tenantId);
    entity.setEventId(eventId);
    entity.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    entity.setStatus("draft");
    entity.setDeleted(0);
    mapper.insert(entity);
    return entity;
  }

  @Transactional(readOnly = true)
  public CompetitionStageEntity getById(Long tenantId, Long stageId) {
    return findExisting(tenantId, stageId);
  }

  @Transactional
  public CompetitionStageEntity update(Long tenantId, Long stageId, CompetitionStageUpdateReq req) {
    CompetitionStageEntity entity = findExisting(tenantId, stageId);
    validateDateRange(req.startAt(), req.endAt());
    CompetitionStageConverter.applyUpdate(entity, req);
    mapper.updateById(entity);
    return entity;
  }

  private CompetitionStageEntity findExisting(Long tenantId, Long stageId) {
    CompetitionStageEntity entity = mapper.selectOne(new LambdaQueryWrapper<CompetitionStageEntity>()
        .eq(CompetitionStageEntity::getTenantId, tenantId)
        .eq(CompetitionStageEntity::getId, stageId));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛段不存在");
    }
    return entity;
  }

  private void validateDateRange(LocalDateTime startAt, LocalDateTime endAt) {
    if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛段结束时间不能早于开始时间");
    }
  }
}
