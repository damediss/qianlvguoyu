package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.domain.CompetitionPromotionRuleDomainService;
import com.guoyu.platform.competition.dto.CompetitionWorkTypeStageConfigReq;
import com.guoyu.platform.competition.dto.CompetitionWorkTypeStageReplaceReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeStageEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.enums.CompetitionStagePlanStatus;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeStageMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetitionWorkTypeStageApplicationService {
  private final CompetitionEventService eventService;
  private final CompetitionEventWorkTypeApplicationService workTypeService;
  private final CompetitionStageService stageService;
  private final CompetitionEventWorkTypeStageMapper mapper;
  private final CompetitionEventWorkTypeMapper workTypeMapper;
  private final CompetitionPromotionRuleDomainService ruleService = new CompetitionPromotionRuleDomainService();

  public CompetitionWorkTypeStageApplicationService(
      CompetitionEventService eventService,
      CompetitionEventWorkTypeApplicationService workTypeService,
      CompetitionStageService stageService,
      CompetitionEventWorkTypeStageMapper mapper,
      CompetitionEventWorkTypeMapper workTypeMapper) {
    this.eventService = eventService;
    this.workTypeService = workTypeService;
    this.stageService = stageService;
    this.mapper = mapper;
    this.workTypeMapper = workTypeMapper;
  }

  @Transactional(readOnly = true)
  public List<CompetitionWorkTypeStageConfigVO> list(Long eventId, Long eventWorkTypeId) {
    workTypeService.assertAccessible(eventId, false);
    Long tenantId = tenant();
    CompetitionEventWorkTypeEntity eventWorkType = requireWorkType(tenantId, eventId, eventWorkTypeId);
    List<CompetitionStageEntity> stages = stageService.list(tenantId, eventId);
    List<CompetitionEventWorkTypeStageEntity> rows = mapper.selectList(new LambdaQueryWrapper<CompetitionEventWorkTypeStageEntity>()
        .eq(CompetitionEventWorkTypeStageEntity::getTenantId, tenantId)
        .eq(CompetitionEventWorkTypeStageEntity::getEventId, eventId)
        .eq(CompetitionEventWorkTypeStageEntity::getEventWorkTypeId, eventWorkTypeId)
        .eq(CompetitionEventWorkTypeStageEntity::getDeleted, 0)
        .orderByAsc(CompetitionEventWorkTypeStageEntity::getSortOrder)
        .orderByAsc(CompetitionEventWorkTypeStageEntity::getStageId));
    Map<Long, CompetitionEventWorkTypeStageEntity> byStage = new HashMap<>();
    if (rows != null) rows.forEach(row -> byStage.put(row.getStageId(), row));
    List<CompetitionWorkTypeStageConfigVO> result = new ArrayList<>();
    for (CompetitionStageEntity stage : stages) {
      CompetitionEventWorkTypeStageEntity row = byStage.get(stage.getId());
      result.add(toVO(stage, row, eventWorkTypeId));
    }
    return result;
  }

  @Transactional
  public List<CompetitionWorkTypeStageConfigVO> replace(
      Long eventId, Long eventWorkTypeId, CompetitionWorkTypeStageReplaceReq req) {
    workTypeService.assertAccessible(eventId, true);
    Long tenantId = tenant();
    CompetitionEventWorkTypeEntity eventWorkType = requireWorkType(tenantId, eventId, eventWorkTypeId);
    CompetitionEventEntity event = eventService.getById(tenantId, eventId);
    if (!"draft".equals(event.getStatus())
        || CompetitionStagePlanStatus.CONFIRMED.getCode().equals(event.getStagePlanStatus())
        || CompetitionStagePlanStatus.CONFIRMED.getCode().equals(eventWorkType.getStagePlanStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "赛事赛段编排已确认，不能修改工种赛段配置");
    }
    List<CompetitionStageEntity> stages = stageService.list(tenantId, eventId);
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
      CompetitionEventWorkTypeStageEntity row = mapper.selectOne(new LambdaQueryWrapper<CompetitionEventWorkTypeStageEntity>()
          .eq(CompetitionEventWorkTypeStageEntity::getTenantId, tenantId)
          .eq(CompetitionEventWorkTypeStageEntity::getEventWorkTypeId, eventWorkTypeId)
          .eq(CompetitionEventWorkTypeStageEntity::getStageId, item.stageId())
          .eq(CompetitionEventWorkTypeStageEntity::getDeleted, 0));
      if (row == null) {
        row = new CompetitionEventWorkTypeStageEntity();
        row.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
        row.setTenantId(tenantId);
        row.setEventId(eventId);
        row.setEventWorkTypeId(eventWorkTypeId);
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
      CompetitionEventWorkTypeStageEntity row = mapper.selectOne(new LambdaQueryWrapper<CompetitionEventWorkTypeStageEntity>()
          .eq(CompetitionEventWorkTypeStageEntity::getTenantId, tenantId)
          .eq(CompetitionEventWorkTypeStageEntity::getEventWorkTypeId, eventWorkTypeId)
          .eq(CompetitionEventWorkTypeStageEntity::getStageId, stage.getId())
          .eq(CompetitionEventWorkTypeStageEntity::getDeleted, 0));
      if (row != null) { row.setEnabled(0); row.setUpdatedBy(userId()); mapper.updateById(row); }
    }
    return list(eventId, eventWorkTypeId);
  }

  @Transactional
  public CompetitionEventWorkTypeVO confirm(Long eventId, Long eventWorkTypeId) {
    workTypeService.assertAccessible(eventId, true);
    Long tenantId = tenant();
    CompetitionEventWorkTypeEntity eventWorkType = requireWorkType(tenantId, eventId, eventWorkTypeId);
    CompetitionEventEntity event = eventService.getById(tenantId, eventId);
    if (!"draft".equals(event.getStatus())
        || CompetitionStagePlanStatus.CONFIRMED.getCode().equals(event.getStagePlanStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "只有草稿状态赛事允许确认工种赛段编排");
    }
    if (CompetitionStagePlanStatus.CONFIRMED.getCode().equals(eventWorkType.getStagePlanStatus())) {
      return workTypeService.get(eventId, eventWorkTypeId);
    }
    boolean hasEnabledStage = list(eventId, eventWorkTypeId).stream()
        .anyMatch(item -> Objects.equals(item.enabled(), 1));
    if (!hasEnabledStage) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "请先为当前工种保留至少一个赛段");
    }
    eventWorkType.setStagePlanStatus(CompetitionStagePlanStatus.CONFIRMED.getCode());
    eventWorkType.setStagePlanConfirmedBy(userId());
    eventWorkType.setStagePlanConfirmedAt(LocalDateTime.now());
    eventWorkType.setUpdatedBy(userId());
    if (workTypeMapper.updateById(eventWorkType) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "工种赛段编排确认失败，请刷新后重试");
    }
    return workTypeService.get(eventId, eventWorkTypeId);
  }

  private CompetitionWorkTypeStageConfigVO toVO(
      CompetitionStageEntity stage, CompetitionEventWorkTypeStageEntity row, Long eventWorkTypeId) {
    return new CompetitionWorkTypeStageConfigVO(
        row == null ? null : row.getId(), stage.getEventId(), eventWorkTypeId, stage.getId(),
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

  private Long tenant() { return SecurityContext.currentUser().getTenantId(); }
  private Long userId() { return SecurityContext.currentUser().getUserId(); }
}
