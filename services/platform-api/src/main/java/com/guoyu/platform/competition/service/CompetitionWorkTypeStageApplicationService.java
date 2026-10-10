package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.domain.CompetitionPromotionRuleDomainService;
import com.guoyu.platform.competition.dto.CompetitionWorkTypeStageConfigReq;
import com.guoyu.platform.competition.dto.CompetitionWorkTypeStageReplaceReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeStageEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.enums.CompetitionStagePlanStatus;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeStageMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.vo.CompetitionWorkTypeStageConfigVO;
import com.guoyu.platform.competition.vo.CompetitionEventWorkTypeVO;
import com.guoyu.platform.security.SecurityContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetitionWorkTypeStageApplicationService {
  private final CompetitionEventService eventService;
  private final CompetitionEventWorkTypeApplicationService workTypeService;
  private final CompetitionStageService stageService;
  private final CompetitionEventWorkTypeStageMapper mapper;
  private final CompetitionEventWorkTypeMapper workTypeMapper;
  private final CompetitionEventWorkTypeLevelMapper levelMapper;
  private final CompetitionEventPublishReadinessService publishReadinessService;
  private final CompetitionPromotionRuleDomainService ruleService = new CompetitionPromotionRuleDomainService();

  public CompetitionWorkTypeStageApplicationService(
      CompetitionEventService eventService,
      CompetitionEventWorkTypeApplicationService workTypeService,
      CompetitionStageService stageService,
      CompetitionEventWorkTypeStageMapper mapper,
      CompetitionEventWorkTypeMapper workTypeMapper) {
    this(eventService, workTypeService, stageService, mapper, workTypeMapper, null, null);
  }

  public CompetitionWorkTypeStageApplicationService(
      CompetitionEventService eventService,
      CompetitionEventWorkTypeApplicationService workTypeService,
      CompetitionStageService stageService,
      CompetitionEventWorkTypeStageMapper mapper,
      CompetitionEventWorkTypeMapper workTypeMapper,
      CompetitionEventWorkTypeLevelMapper levelMapper) {
    this(eventService, workTypeService, stageService, mapper, workTypeMapper, levelMapper, null);
  }

  @Autowired
  public CompetitionWorkTypeStageApplicationService(
      CompetitionEventService eventService,
      CompetitionEventWorkTypeApplicationService workTypeService,
      CompetitionStageService stageService,
      CompetitionEventWorkTypeStageMapper mapper,
      CompetitionEventWorkTypeMapper workTypeMapper,
      CompetitionEventWorkTypeLevelMapper levelMapper,
      CompetitionEventPublishReadinessService publishReadinessService) {
    this.eventService = eventService;
    this.workTypeService = workTypeService;
    this.stageService = stageService;
    this.mapper = mapper;
    this.workTypeMapper = workTypeMapper;
    this.levelMapper = levelMapper;
    this.publishReadinessService = publishReadinessService;
  }

  @Transactional(readOnly = true)
  public List<CompetitionWorkTypeStageConfigVO> list(Long eventId, Long eventWorkTypeId) {
    return list(eventId, eventWorkTypeId, null);
  }

  @Transactional(readOnly = true)
  public List<CompetitionWorkTypeStageConfigVO> list(
      Long eventId, Long eventWorkTypeId, String eventWorkTypeLevelCode) {
    workTypeService.assertAccessible(eventId, false);
    Long tenantId = tenant();
    CompetitionEventWorkTypeEntity eventWorkType = requireWorkType(tenantId, eventId, eventWorkTypeId);
    String normalizedLevelCode = normalizeLevelCode(eventWorkTypeLevelCode);
    List<CompetitionStageEntity> stages = normalizedLevelCode == null
        ? stageService.list(tenantId, eventId)
        : stageService.list(tenantId, eventId, eventWorkTypeId, normalizedLevelCode);
    List<CompetitionEventWorkTypeStageEntity> rows = mapper.selectList(stageScopeWrapper(
        tenantId, eventId, eventWorkTypeId, normalizedLevelCode)
        .orderByAsc(CompetitionEventWorkTypeStageEntity::getSortOrder)
        .orderByAsc(CompetitionEventWorkTypeStageEntity::getStageId));
    Map<Long, CompetitionEventWorkTypeStageEntity> byStage = new HashMap<>();
    if (rows != null) {
      rows.forEach(row -> {
        if (row != null && row.getStageId() != null) byStage.put(row.getStageId(), row);
      });
    }
    List<CompetitionWorkTypeStageConfigVO> result = new ArrayList<>();
    for (CompetitionStageEntity stage : stages) {
      CompetitionEventWorkTypeStageEntity row = byStage.get(stage.getId());
      result.add(toVO(stage, row, eventWorkTypeId, normalizedLevelCode));
    }
    return result;
  }

  @Transactional
  public List<CompetitionWorkTypeStageConfigVO> replace(
      Long eventId, Long eventWorkTypeId, CompetitionWorkTypeStageReplaceReq req) {
    return replace(eventId, eventWorkTypeId, null, req);
  }

  @Transactional
  public List<CompetitionWorkTypeStageConfigVO> replace(
      Long eventId,
      Long eventWorkTypeId,
      String eventWorkTypeLevelCode,
      CompetitionWorkTypeStageReplaceReq req) {
    workTypeService.assertAccessible(eventId, true);
    Long tenantId = tenant();
    CompetitionEventWorkTypeEntity eventWorkType = requireWorkType(tenantId, eventId, eventWorkTypeId);
    String normalizedLevelCode = normalizeLevelCode(eventWorkTypeLevelCode);
    CompetitionEventEntity event = eventService.getById(tenantId, eventId);
    com.guoyu.platform.competition.domain.CompetitionEventArrangementGuard.requireOpen(
        event, "已关闭或已归档赛事不能修改工种赛段配置");
    if (normalizedLevelCode == null
        && CompetitionStagePlanStatus.CONFIRMED.getCode().equals(eventWorkType.getStagePlanStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "赛事赛段编排已确认，不能修改工种赛段配置");
    }
    CompetitionEventEntity locked = eventService.lockForUpdate(tenantId, eventId);
    if (locked != null) {
      event = locked;
      com.guoyu.platform.competition.domain.CompetitionEventArrangementGuard.requireOpen(
          event, "已关闭或已归档赛事不能修改工种赛段配置");
    }
    assertLevelStagePlanDraft(tenantId, eventId, eventWorkTypeId, normalizedLevelCode);
    List<CompetitionStageEntity> stages = normalizedLevelCode == null
        ? stageService.list(tenantId, eventId)
        : stageService.list(tenantId, eventId, eventWorkTypeId, normalizedLevelCode);
    Map<Long, CompetitionStageEntity> stageById = new HashMap<>();
    stages.forEach(stage -> stageById.put(stage.getId(), stage));
    Set<Long> seen = new HashSet<>();
    for (CompetitionWorkTypeStageConfigReq item : req.stages()) {
      CompetitionStageEntity stage = stageById.get(item.stageId());
      if (stage == null || !seen.add(item.stageId())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "工种赛段配置包含重复或不属于赛事的赛段");
      }
      CompetitionPromotionRuleDomainService.ResolvedRule rule = ruleService.resolve(
          item.promotionRuleType(), item.promotionQuota(), item.promotionPercentage());
      CompetitionEventWorkTypeStageEntity row = mapper.selectOne(stageRowWrapper(
          tenantId, eventId, eventWorkTypeId, normalizedLevelCode, item.stageId()));
      if (row == null) {
        row = new CompetitionEventWorkTypeStageEntity();
        row.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
        row.setTenantId(tenantId);
        row.setEventId(eventId);
        row.setEventWorkTypeId(eventWorkTypeId);
        row.setEventWorkTypeLevelCode(normalizedLevelCode);
        row.setStageId(item.stageId());
        row.setCreatedBy(userId());
        row.setDeleted(0);
      }
      row.setEnabled(Boolean.TRUE.equals(item.enabled()) ? 1 : 0);
      row.setSortOrder(item.sortOrder() == null ? stage.getSortOrder() : item.sortOrder());
      row.setPromotionRuleType(rule.type());
      row.setPromotionPercentage(rule.percentage());
      row.setPromotionQuota(rule.quota());
      row.setLegacyRule(rule.legacy() ? 1 : 0);
      row.setUpdatedBy(userId());
      if (row.getCreatedAt() == null) mapper.insert(row); else mapper.updateById(row);
    }
    for (CompetitionStageEntity stage : stages) {
      if (seen.contains(stage.getId())) continue;
      CompetitionEventWorkTypeStageEntity row = mapper.selectOne(stageRowWrapper(
          tenantId, eventId, eventWorkTypeId, normalizedLevelCode, stage.getId()));
      if (row != null) { row.setEnabled(0); row.setUpdatedBy(userId()); mapper.updateById(row); }
    }
    return list(eventId, eventWorkTypeId, normalizedLevelCode);
  }

  @Transactional
  public CompetitionEventWorkTypeVO confirm(Long eventId, Long eventWorkTypeId) {
    return confirm(eventId, eventWorkTypeId, null);
  }

  @Transactional
  public CompetitionEventWorkTypeVO confirm(
      Long eventId, Long eventWorkTypeId, String eventWorkTypeLevelCode) {
    workTypeService.assertAccessible(eventId, true);
    Long tenantId = tenant();
    CompetitionEventWorkTypeEntity eventWorkType = requireWorkType(tenantId, eventId, eventWorkTypeId);
    String normalizedLevelCode = normalizeLevelCode(eventWorkTypeLevelCode);
    CompetitionEventEntity event = eventService.getById(tenantId, eventId);
    com.guoyu.platform.competition.domain.CompetitionEventArrangementGuard.requireOpen(
        event, "已关闭或已归档赛事不能确认工种赛段编排");
    CompetitionEventEntity locked = eventService.lockForUpdate(tenantId, eventId);
    if (locked != null) {
      event = locked;
      com.guoyu.platform.competition.domain.CompetitionEventArrangementGuard.requireOpen(
          event, "已关闭或已归档赛事不能确认工种赛段编排");
    }

    if (normalizedLevelCode == null) {
      if (CompetitionStagePlanStatus.CONFIRMED.getCode().equals(eventWorkType.getStagePlanStatus())) {
        return workTypeService.get(eventId, eventWorkTypeId);
      }
      boolean hasEnabledStage = list(eventId, eventWorkTypeId, null).stream()
          .anyMatch(item -> Objects.equals(item.enabled(), 1));
      if (!hasEnabledStage) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "请先为当前工种保留至少一个赛段");
      }
      confirmWorkType(eventWorkType);
      return workTypeService.get(eventId, eventWorkTypeId);
    }

    CompetitionEventWorkTypeLevelEntity level = findLevel(
        tenantId, eventId, eventWorkTypeId, normalizedLevelCode);
    if (level == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "当前等级未在赛事总览中明确选择，不能确认赛段编排");
    }
    if (level != null
        && CompetitionStagePlanStatus.CONFIRMED.getCode().equals(level.getStagePlanStatus())) {
      return workTypeService.get(eventId, eventWorkTypeId);
    }
    if (publishReadinessService != null) {
      var readiness = publishReadinessService.evaluateForScopeConfirmation(
          tenantId, event, eventWorkType, level);
      if (!readiness.ready()) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            publishReadinessService.firstBlockingMessage(readiness));
      }
    } else {
      boolean hasEnabledStage = list(eventId, eventWorkTypeId, normalizedLevelCode).stream()
          .anyMatch(item -> Objects.equals(item.enabled(), 1));
      if (!hasEnabledStage) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "请先为当前工种当前等级保留至少一个赛段");
      }
    }
    level.setStagePlanStatus(CompetitionStagePlanStatus.CONFIRMED.getCode());
    level.setStagePlanConfirmedBy(userId());
    level.setStagePlanConfirmedAt(LocalDateTime.now());
    level.setUpdatedBy(userId());
    if (levelMapper.updateById(level) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "工种当前等级赛段编排确认失败，请刷新后重试");
    }
    return workTypeService.get(eventId, eventWorkTypeId);
  }

  private void confirmWorkType(CompetitionEventWorkTypeEntity eventWorkType) {
    eventWorkType.setStagePlanStatus(CompetitionStagePlanStatus.CONFIRMED.getCode());
    eventWorkType.setStagePlanConfirmedBy(userId());
    eventWorkType.setStagePlanConfirmedAt(LocalDateTime.now());
    eventWorkType.setUpdatedBy(userId());
    if (workTypeMapper.updateById(eventWorkType) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "工种赛段编排确认失败，请刷新后重试");
    }
  }

  private void assertLevelStagePlanDraft(
      Long tenantId, Long eventId, Long eventWorkTypeId, String eventWorkTypeLevelCode) {
    if (eventWorkTypeLevelCode == null || levelMapper == null) {
      return;
    }
    CompetitionEventWorkTypeLevelEntity level = findLevel(
        tenantId, eventId, eventWorkTypeId, eventWorkTypeLevelCode);
    if (level != null
        && CompetitionStagePlanStatus.CONFIRMED.getCode().equals(level.getStagePlanStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前工种当前等级赛段编排已确认，不能继续修改");
    }
  }

  private CompetitionEventWorkTypeLevelEntity findLevel(
      Long tenantId, Long eventId, Long eventWorkTypeId, String eventWorkTypeLevelCode) {
    if (levelMapper == null) {
      return null;
    }
    return levelMapper.selectOne(new LambdaQueryWrapper<CompetitionEventWorkTypeLevelEntity>()
        .eq(CompetitionEventWorkTypeLevelEntity::getTenantId, tenantId)
        .eq(CompetitionEventWorkTypeLevelEntity::getEventId, eventId)
        .eq(CompetitionEventWorkTypeLevelEntity::getEventWorkTypeId, eventWorkTypeId)
        .eq(CompetitionEventWorkTypeLevelEntity::getLevelCode, eventWorkTypeLevelCode)
        .eq(CompetitionEventWorkTypeLevelEntity::getDeleted, 0));
  }

  private CompetitionWorkTypeStageConfigVO toVO(
      CompetitionStageEntity stage,
      CompetitionEventWorkTypeStageEntity row,
      Long eventWorkTypeId,
      String eventWorkTypeLevelCode) {
    return new CompetitionWorkTypeStageConfigVO(
        row == null ? null : row.getId(), stage.getEventId(), eventWorkTypeId,
        eventWorkTypeLevelCode, stage.getId(),
        stage.getStageType(), stage.getName(), row == null ? 1 : row.getEnabled(),
        row == null ? stage.getSortOrder() : row.getSortOrder(),
        row == null ? CompetitionPromotionRuleDomainService.LEGACY : row.getPromotionRuleType(),
        row == null ? null : row.getPromotionPercentage(),
        row == null ? null : row.getPromotionQuota(),
        row == null ? 1 : row.getLegacyRule());
  }

  private CompetitionEventWorkTypeEntity requireWorkType(Long tenantId, Long eventId, Long eventWorkTypeId) {
    CompetitionEventWorkTypeEntity workType = workTypeMapper.selectOne(new LambdaQueryWrapper<CompetitionEventWorkTypeEntity>()
        .eq(CompetitionEventWorkTypeEntity::getTenantId, tenantId)
        .eq(CompetitionEventWorkTypeEntity::getEventId, eventId)
        .eq(CompetitionEventWorkTypeEntity::getId, eventWorkTypeId)
        .eq(CompetitionEventWorkTypeEntity::getDeleted, 0));
    if (workType == null) throw new BusinessException(ErrorCode.NOT_FOUND, "赛事工种不存在");
    return workType;
  }

  private LambdaQueryWrapper<CompetitionEventWorkTypeStageEntity> stageScopeWrapper(
      Long tenantId, Long eventId, Long eventWorkTypeId, String eventWorkTypeLevelCode) {
    LambdaQueryWrapper<CompetitionEventWorkTypeStageEntity> wrapper =
        new LambdaQueryWrapper<CompetitionEventWorkTypeStageEntity>()
            .eq(CompetitionEventWorkTypeStageEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeStageEntity::getEventId, eventId)
            .eq(CompetitionEventWorkTypeStageEntity::getEventWorkTypeId, eventWorkTypeId)
            .eq(CompetitionEventWorkTypeStageEntity::getDeleted, 0);
    if (eventWorkTypeLevelCode == null) {
      wrapper.isNull(CompetitionEventWorkTypeStageEntity::getEventWorkTypeLevelCode);
    } else {
      wrapper.eq(CompetitionEventWorkTypeStageEntity::getEventWorkTypeLevelCode, eventWorkTypeLevelCode);
    }
    return wrapper;
  }

  private LambdaQueryWrapper<CompetitionEventWorkTypeStageEntity> stageRowWrapper(
      Long tenantId,
      Long eventId,
      Long eventWorkTypeId,
      String eventWorkTypeLevelCode,
      Long stageId) {
    return stageScopeWrapper(tenantId, eventId, eventWorkTypeId, eventWorkTypeLevelCode)
        .eq(CompetitionEventWorkTypeStageEntity::getStageId, stageId);
  }

  private String normalizeLevelCode(String eventWorkTypeLevelCode) {
    if (eventWorkTypeLevelCode == null) return null;
    String value = eventWorkTypeLevelCode.trim();
    if (!Set.of("1", "2", "3", "4", "5").contains(value)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛事工种等级编码必须是1至5");
    }
    return value;
  }

  private Long tenant() { return SecurityContext.currentUser().getTenantId(); }
  private Long userId() { return SecurityContext.currentUser().getUserId(); }
}
