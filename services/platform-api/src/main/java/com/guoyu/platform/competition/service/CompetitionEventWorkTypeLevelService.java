package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.converter.CompetitionEventWorkTypeLevelConverter;
import com.guoyu.platform.competition.dto.CompetitionEventWorkTypeLevelBatchReq;
import com.guoyu.platform.competition.dto.CompetitionEventWorkTypeLevelItemReq;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionEventWorkTypeLevelService {

  private final CompetitionEventWorkTypeLevelMapper mapper;
  private final CompetitionEventWorkTypeDomainService workTypeDomainService;

  public CompetitionEventWorkTypeLevelService(
      CompetitionEventWorkTypeLevelMapper mapper,
      CompetitionEventWorkTypeDomainService workTypeDomainService) {
    this.mapper = mapper;
    this.workTypeDomainService = workTypeDomainService;
  }

  @Transactional(readOnly = true)
  public List<CompetitionEventWorkTypeLevelEntity> list(
      Long tenantId, Long eventId, Long eventWorkTypeId) {
    requireWorkType(tenantId, eventId, eventWorkTypeId);
    List<CompetitionEventWorkTypeLevelEntity> records = mapper.selectList(
        new LambdaQueryWrapper<CompetitionEventWorkTypeLevelEntity>()
            .eq(CompetitionEventWorkTypeLevelEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeLevelEntity::getEventId, eventId)
            .eq(CompetitionEventWorkTypeLevelEntity::getEventWorkTypeId, eventWorkTypeId)
            .eq(CompetitionEventWorkTypeLevelEntity::getDeleted, 0)
            .orderByAsc(CompetitionEventWorkTypeLevelEntity::getLevelCode));
    return records == null ? new ArrayList<>() : new ArrayList<>(records);
  }

  @Transactional
  public List<CompetitionEventWorkTypeLevelEntity> replaceAll(
      Long tenantId,
      Long eventId,
      Long eventWorkTypeId,
      CompetitionEventWorkTypeLevelBatchReq req,
      Long operatorId) {
    CompetitionEventWorkTypeEntity workType = requireWorkType(tenantId, eventId, eventWorkTypeId);
    if (req == null || req.levels() == null || req.levels().isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "至少需要配置一个赛事工种等级");
    }
    if (req.levels().size() > 5
        || req.levels().stream().map(CompetitionEventWorkTypeLevelItemReq::levelCode).collect(Collectors.toSet()).size()
            != req.levels().size()) {
      throw new BusinessException(ErrorCode.CONFLICT, "等级必须是1-5且不能重复");
    }
    if (req.levels().stream().anyMatch(item -> !SetSupport.STANDARD_CODES.contains(item.levelCode()))) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛事工种等级编码必须是1至5");
    }

    List<CompetitionEventWorkTypeLevelEntity> existing = mapper.selectList(
        new LambdaQueryWrapper<CompetitionEventWorkTypeLevelEntity>()
            .eq(CompetitionEventWorkTypeLevelEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeLevelEntity::getEventId, eventId)
            .eq(CompetitionEventWorkTypeLevelEntity::getEventWorkTypeId, eventWorkTypeId)
            .eq(CompetitionEventWorkTypeLevelEntity::getDeleted, 0));
    if (existing == null) {
      existing = new ArrayList<>();
    }
    Map<Long, CompetitionEventWorkTypeLevelEntity> existingById = existing.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(CompetitionEventWorkTypeLevelEntity::getId, Function.identity()));
    Map<String, CompetitionEventWorkTypeLevelEntity> existingByCode = existing.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(CompetitionEventWorkTypeLevelEntity::getLevelCode, Function.identity()));
    HashSet<Long> retainedIds = new HashSet<>();
    List<CompetitionEventWorkTypeLevelEntity> result = new ArrayList<>();
    for (CompetitionEventWorkTypeLevelItemReq item : req.levels()) {
      CompetitionEventWorkTypeLevelEntity entity = item.id() == null
          ? existingByCode.get(item.levelCode()) : existingById.get(item.id());
      if (entity == null) {
        entity = new CompetitionEventWorkTypeLevelEntity();
        entity.setTenantId(tenantId);
        entity.setEventId(eventId);
        entity.setEventWorkTypeId(workType.getId());
        entity.setDeleted(0);
        CompetitionEventWorkTypeLevelConverter.apply(entity, item);
        entity.setConfirmedBy(operatorId);
        entity.setConfirmedAt("confirmed".equals(entity.getReconciliationStatus())
            ? LocalDateTime.now() : null);
        try {
          if (mapper.insert(entity) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "等级保存失败，请刷新后重试");
          }
        } catch (DuplicateKeyException ex) {
          throw new BusinessException(ErrorCode.CONFLICT, "赛事工种等级已发生变化，请刷新后重试");
        }
      } else {
        if (!Objects.equals(entity.getEventId(), eventId)
            || !Objects.equals(entity.getEventWorkTypeId(), eventWorkTypeId)) {
          throw new BusinessException(ErrorCode.NOT_FOUND, "赛事工种等级不存在");
        }
        CompetitionEventWorkTypeLevelConverter.apply(entity, item);
        entity.setConfirmedBy(operatorId);
        entity.setConfirmedAt("confirmed".equals(entity.getReconciliationStatus())
            ? (entity.getConfirmedAt() == null ? LocalDateTime.now() : entity.getConfirmedAt()) : null);
        if (mapper.updateById(entity) != 1) {
          throw new BusinessException(ErrorCode.CONFLICT, "等级已发生变化，请刷新后重试");
        }
      }
      retainedIds.add(entity.getId());
      result.add(entity);
    }
    for (CompetitionEventWorkTypeLevelEntity entity : existing) {
      if (entity != null && !retainedIds.contains(entity.getId()) && mapper.deleteById(entity.getId()) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "等级删除失败，请刷新后重试");
      }
    }
    return result;
  }

  private CompetitionEventWorkTypeEntity requireWorkType(Long tenantId, Long eventId, Long eventWorkTypeId) {
    CompetitionEventWorkTypeEntity entity = workTypeDomainService.getById(tenantId, eventWorkTypeId);
    if (entity == null || !Objects.equals(entity.getEventId(), eventId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事工种不存在");
    }
    return entity;
  }

  private static final class SetSupport {
    private static final java.util.Set<String> STANDARD_CODES =
        java.util.Set.of("1", "2", "3", "4", "5");

    private SetSupport() {
    }
  }
}
