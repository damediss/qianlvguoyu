package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.converter.CompetitionStageConverter;
import com.guoyu.platform.competition.domain.CompetitionStageLifecycleGuard;
import com.guoyu.platform.competition.domain.CompetitionStageLifecycleDomainService;
import com.guoyu.platform.competition.dto.CompetitionStageCreateReq;
import com.guoyu.platform.competition.dto.CompetitionStageDraftSaveReq;
import com.guoyu.platform.competition.dto.CompetitionStageDraftStageReq;
import com.guoyu.platform.competition.dto.CompetitionStageQueryReq;
import com.guoyu.platform.competition.dto.CompetitionStageUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.entity.CompetitionStageLifecycleHistoryEntity;
import com.guoyu.platform.competition.enums.CompetitionStageStatus;
import com.guoyu.platform.competition.enums.CompetitionStageType;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageLifecycleHistoryMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionStageService {

  private final CompetitionStageMapper mapper;
  private final CompetitionStageLifecycleDomainService lifecycleDomainService;
  private final CompetitionStageLifecycleHistoryMapper lifecycleHistoryMapper;
  private final CompetitionStageLifecycleGuard lifecycleGuard;
  private final CompetitionEventMapper eventMapper;

  public CompetitionStageService(CompetitionStageMapper mapper) {
    this(mapper, new CompetitionStageLifecycleDomainService(), null, null, null);
  }

  @Autowired
  public CompetitionStageService(
      CompetitionStageMapper mapper,
      CompetitionStageLifecycleDomainService lifecycleDomainService,
      CompetitionStageLifecycleHistoryMapper lifecycleHistoryMapper,
      CompetitionStageLifecycleGuard lifecycleGuard,
      CompetitionEventMapper eventMapper) {
    this.mapper = mapper;
    this.lifecycleDomainService = lifecycleDomainService;
    this.lifecycleHistoryMapper = lifecycleHistoryMapper;
    this.lifecycleGuard = lifecycleGuard;
    this.eventMapper = eventMapper;
  }

  @Transactional(readOnly = true)
  public Page<CompetitionStageEntity> page(Long tenantId, Long eventId, CompetitionStageQueryReq req) {
    return page(tenantId, eventId, req.getEventWorkTypeId(), req.getEventWorkTypeLevelCode(), req);
  }

  @Transactional(readOnly = true)
  public Page<CompetitionStageEntity> page(
      Long tenantId,
      Long eventId,
      Long eventWorkTypeId,
      String eventWorkTypeLevelCode,
      CompetitionStageQueryReq req) {
    String normalizedLevelCode = normalizeScopeLevelCode(
        eventWorkTypeId, eventWorkTypeLevelCode);
    LambdaQueryWrapper<CompetitionStageEntity> wrapper = stageScopeWrapper(
        tenantId, eventId, eventWorkTypeId, normalizedLevelCode)
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

  @Transactional(readOnly = true)
  public List<CompetitionStageEntity> list(Long tenantId, Long eventId) {
    return list(tenantId, eventId, null, null);
  }

  @Transactional(readOnly = true)
  public List<CompetitionStageEntity> list(
      Long tenantId, Long eventId, Long eventWorkTypeId, String eventWorkTypeLevelCode) {
    String normalizedLevelCode = normalizeScopeLevelCode(
        eventWorkTypeId, eventWorkTypeLevelCode);
    List<CompetitionStageEntity> stages = mapper.selectList(stageScopeWrapper(
        tenantId, eventId, eventWorkTypeId, normalizedLevelCode)
        .orderByAsc(CompetitionStageEntity::getSortOrder)
        .orderByAsc(CompetitionStageEntity::getId));
    if (stages == null) {
      log.warn("Competition stage mapper returned null list, tenantId={}, eventId={}", tenantId, eventId);
      return new ArrayList<>();
    }
    return stages;
  }

  @Transactional
  public CompetitionStageEntity create(Long tenantId, Long eventId, CompetitionStageCreateReq req) {
    validateDateRange(req.startAt(), req.endAt());
    CompetitionEventEntity event = loadEventForValidation(tenantId, eventId);
    requireDraftArrangementEvent(event);
    validateEventWindow(event, req.startAt(), req.endAt());
    CompetitionStageType stageType = validateRequest(tenantId, eventId, null,
        req.name(), req.stageType(), req.sortOrder(), req.startAt(), req.endAt());
    CompetitionStageEntity entity = CompetitionStageConverter.toEntity(req);
    entity.setTenantId(tenantId);
    entity.setEventId(eventId);
    entity.setName(stageType.getDisplayName());
    entity.setStageType(stageType.getCode());
    entity.setSortOrder(stageType.getSortOrder());
    entity.setStatus(CompetitionStageStatus.DRAFT.getCode());
    entity.setDeleted(0);
    if (mapper.insert(entity) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "赛段创建失败，请刷新后重试");
    }
    return entity;
  }

  /**
   * Atomically replaces the complete draft stage structure.
   *
   * <p>The caller has already established the authenticated user's event access and draft event
   * state. This service owns the complete-array validation and the persistence diff so a malformed
   * graph can never result in a partially saved stage structure.</p>
   */
  @Transactional
  public List<CompetitionStageEntity> replaceDraft(
      Long tenantId, Long eventId, Long userId, CompetitionStageDraftSaveReq req) {
    return replaceDraft(tenantId, eventId, userId, null, null, req);
  }

  /**
   * Replaces the complete draft for one event-work-type-level canvas.
   *
   * <p>Each scope owns its own stage rows (and therefore its own stage content rows). The
   * legacy overload above deliberately keeps the event-level {@code NULL/NULL} scope.</p>
   */
  @Transactional
  public List<CompetitionStageEntity> replaceDraftForScope(
      Long tenantId,
      Long eventId,
      Long userId,
      Long eventWorkTypeId,
      String eventWorkTypeLevelCode,
      CompetitionStageDraftSaveReq req) {
    String normalizedLevelCode = normalizeScopeLevelCode(
        eventWorkTypeId, eventWorkTypeLevelCode);
    if (normalizedLevelCode == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛事工种等级编码必须是1至5");
    }
    return replaceDraft(
        tenantId, eventId, userId, eventWorkTypeId, normalizedLevelCode, req);
  }

  private List<CompetitionStageEntity> replaceDraft(
      Long tenantId,
      Long eventId,
      Long userId,
      Long eventWorkTypeId,
      String eventWorkTypeLevelCode,
      CompetitionStageDraftSaveReq req) {
    if (req == null || req.stages() == null || req.stages().isEmpty()
        || req.stages().size() > CompetitionStageType.values().length) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛事必须配置1至5个赛段");
    }

    CompetitionEventEntity event = loadEventForValidation(tenantId, eventId);
    requireDraftArrangementEvent(event);

    List<CompetitionStageEntity> existing = mapper.selectList(stageScopeWrapper(
        tenantId, eventId, eventWorkTypeId, eventWorkTypeLevelCode)
        .orderByAsc(CompetitionStageEntity::getSortOrder)
        .orderByAsc(CompetitionStageEntity::getId));
    if (existing == null) {
      log.warn("Competition stage mapper returned null list during draft replacement, tenantId={}, eventId={}",
          tenantId, eventId);
      existing = new ArrayList<>();
    }

    Map<Long, CompetitionStageEntity> existingById = new HashMap<>();
    for (CompetitionStageEntity stage : existing) {
      if (stage != null && stage.getId() != null) {
        existingById.put(stage.getId(), stage);
      }
    }

    Set<String> requestedTypes = new HashSet<>();
    Set<Long> retainedIds = new HashSet<>();
    List<StageWindow> windows = new ArrayList<>();
    for (CompetitionStageDraftStageReq item : req.stages()) {
      if (item == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "赛段数组不能包含空元素");
      }
      CompetitionStageType stageType = CompetitionStageType.fromCode(item.stageType());
      if (stageType == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "赛段类型必须是固定预设类型");
      }
      if (!requestedTypes.add(stageType.getCode())) {
        throw new BusinessException(ErrorCode.CONFLICT, "同一赛事不能重复配置同一预设赛段");
      }
      validateDateRange(item.startAt(), item.endAt());
      validateEventWindow(event, item.startAt(), item.endAt());
      if (item.startAt() != null && item.endAt() != null) {
        windows.add(new StageWindow(stageType, item.startAt(), item.endAt()));
      }

      if (item.id() != null) {
        CompetitionStageEntity existingStage = existingById.get(item.id());
        if (existingStage == null) {
          log.warn("Stage draft save contained a stage outside event scope, tenantId={}, eventId={}, stageId={}",
              tenantId, eventId, item.id());
          throw new BusinessException(ErrorCode.FORBIDDEN, "赛段不属于当前租户或赛事");
        }
        if (!eventId.equals(existingStage.getEventId())) {
          log.warn("Stage draft save contained a cross-event stage id, tenantId={}, eventId={}, stageId={}, actualEventId={}",
              tenantId, eventId, item.id(), existingStage.getEventId());
          throw new BusinessException(ErrorCode.CONFLICT, "赛段不属于当前赛事");
        }
        if (!stageType.getCode().equals(existingStage.getStageType())) {
          throw new BusinessException(ErrorCode.CONFLICT, "已有赛段的类型不能在批量保存中替换");
        }
        lifecycleDomainService.requireWritable(existingStage.getStatus());
        retainedIds.add(item.id());
      }
    }

    windows.sort(java.util.Comparator.comparingInt(window -> window.type().getSortOrder()));
    for (int index = 1; index < windows.size(); index++) {
      StageWindow previous = windows.get(index - 1);
      StageWindow current = windows.get(index);
      if (current.startAt().isBefore(previous.endAt())) {
        throw new BusinessException(ErrorCode.CONFLICT, "赛段时间窗口重叠或未按预设顺序递增");
      }
    }

    for (CompetitionStageEntity stage : existing) {
      if (stage == null || retainedIds.contains(stage.getId())) {
        continue;
      }
      if (!CompetitionStageStatus.DRAFT.getCode().equals(stage.getStatus())) {
        throw new BusinessException(ErrorCode.CONFLICT, "只有草稿赛段允许删除");
      }
    }

    for (CompetitionStageDraftStageReq item : req.stages()) {
      CompetitionStageType stageType = CompetitionStageType.fromCode(item.stageType());
      if (item.id() != null) {
        CompetitionStageEntity entity = existingById.get(item.id());
        entity.setStartAt(item.startAt());
        entity.setEndAt(item.endAt());
        entity.setDescription(item.description());
        entity.setUpdatedBy(userId);
        if (mapper.updateById(entity) != 1) {
          throw new BusinessException(ErrorCode.CONFLICT, "赛段已发生变化，请刷新后重试");
        }
      } else {
        CompetitionStageEntity entity = new CompetitionStageEntity();
        entity.setTenantId(tenantId);
        entity.setEventId(eventId);
        entity.setEventWorkTypeId(eventWorkTypeId);
        entity.setEventWorkTypeLevelCode(eventWorkTypeLevelCode);
        entity.setName(stageType.getDisplayName());
        entity.setStageType(stageType.getCode());
        entity.setSortOrder(stageType.getSortOrder());
        entity.setStartAt(item.startAt());
        entity.setEndAt(item.endAt());
        entity.setStatus(CompetitionStageStatus.DRAFT.getCode());
        entity.setDescription(item.description());
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        entity.setDeleted(0);
        if (mapper.insert(entity) != 1) {
          throw new BusinessException(ErrorCode.CONFLICT, "赛段创建失败，请刷新后重试");
        }
      }
    }

    for (CompetitionStageEntity stage : existing) {
      if (stage != null && !retainedIds.contains(stage.getId())
          && mapper.deleteById(stage.getId()) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "赛段已发生变化，请刷新后重试");
      }
    }

    List<CompetitionStageEntity> saved = mapper.selectList(stageScopeWrapper(
        tenantId, eventId, eventWorkTypeId, eventWorkTypeLevelCode)
        .orderByAsc(CompetitionStageEntity::getSortOrder)
        .orderByAsc(CompetitionStageEntity::getId));
    if (saved == null) {
      log.warn("Competition stage mapper returned null result after draft replacement, tenantId={}, eventId={}",
          tenantId, eventId);
      return new ArrayList<>();
    }
    return saved;
  }

  @Transactional(readOnly = true)
  public CompetitionStageEntity getById(Long tenantId, Long stageId) {
    return findExisting(tenantId, stageId);
  }

  /** Locks the parent event so content writes cannot race the event publish command. */
  @Transactional
  public CompetitionEventEntity lockEventForArrangementUpdate(Long tenantId, Long eventId) {
    return loadEventForValidation(tenantId, eventId);
  }

  @Transactional
  public CompetitionStageEntity update(Long tenantId, Long stageId, CompetitionStageUpdateReq req) {
    CompetitionStageEntity entity = findExisting(tenantId, stageId);
    validateDateRange(req.startAt(), req.endAt());
    CompetitionEventEntity event = loadEventForValidation(tenantId, entity.getEventId());
    requireDraftArrangementEvent(event);
    validateEventWindow(event, req.startAt(), req.endAt());
    lifecycleDomainService.requireWritable(entity.getStatus());
    CompetitionStageType stageType = validateRequest(tenantId, entity.getEventId(), stageId,
        req.name(), req.stageType(), req.sortOrder(), req.startAt(), req.endAt());
    CompetitionStageConverter.applyUpdate(entity, req);
    entity.setName(stageType.getDisplayName());
    entity.setStageType(stageType.getCode());
    entity.setSortOrder(stageType.getSortOrder());
    entity.setStatus(CompetitionStageStatus.fromCode(entity.getStatus()) == null
        ? CompetitionStageStatus.DRAFT.getCode() : entity.getStatus());
    if (mapper.updateById(entity) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "赛段已发生变化，请刷新后重试");
    }
    return entity;
  }

  @Transactional
  public void delete(Long tenantId, Long stageId) {
    CompetitionStageEntity entity = findExisting(tenantId, stageId);
    requireDraftArrangementEvent(loadEventForValidation(tenantId, entity.getEventId()));
    if (!CompetitionStageStatus.DRAFT.getCode().equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "只有草稿赛段允许删除");
    }
    long stageCount = mapper.selectCount(new LambdaQueryWrapper<CompetitionStageEntity>()
        .eq(CompetitionStageEntity::getTenantId, tenantId)
        .eq(CompetitionStageEntity::getEventId, entity.getEventId())
        .eq(CompetitionStageEntity::getDeleted, 0)
        .eq(entity.getEventWorkTypeId() != null,
            CompetitionStageEntity::getEventWorkTypeId, entity.getEventWorkTypeId())
        .isNull(entity.getEventWorkTypeId() == null,
            CompetitionStageEntity::getEventWorkTypeId)
        .eq(entity.getEventWorkTypeLevelCode() != null,
            CompetitionStageEntity::getEventWorkTypeLevelCode, entity.getEventWorkTypeLevelCode())
        .isNull(entity.getEventWorkTypeLevelCode() == null,
            CompetitionStageEntity::getEventWorkTypeLevelCode));
    if (stageCount <= 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "赛事至少需要保留一个赛段");
    }
    if (mapper.deleteById(stageId) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "赛段已发生变化，请刷新后重试");
    }
  }

  @Transactional
  public CompetitionStageEntity beginConfiguration(Long tenantId, Long userId, Long stageId, String reason) {
    return changeStatus(tenantId, userId, stageId, CompetitionStageStatus.DRAFT,
        CompetitionStageStatus.CONFIGURING, "begin_configuration", reason);
  }

  @Transactional
  public CompetitionStageEntity lockReady(Long tenantId, Long userId, Long stageId) {
    CompetitionStageEntity entity = locked(tenantId, stageId);
    if (lifecycleGuard != null) lifecycleGuard.requireReadyToLock(tenantId, entity);
    return changeStatus(tenantId, userId, stageId, CompetitionStageStatus.CONFIGURING,
        CompetitionStageStatus.READY_LOCKED, "lock_ready", null);
  }

  @Transactional
  public CompetitionStageEntity startStage(Long tenantId, Long userId, Long stageId) {
    CompetitionStageEntity entity = locked(tenantId, stageId);
    if (lifecycleGuard != null) lifecycleGuard.requireStartable(tenantId, entity);
    return changeStatus(tenantId, userId, stageId, CompetitionStageStatus.READY_LOCKED,
        CompetitionStageStatus.IN_PROGRESS, "start_stage", null);
  }

  @Transactional
  public CompetitionStageEntity closeStage(Long tenantId, Long userId, Long stageId) {
    CompetitionStageEntity entity = locked(tenantId, stageId);
    if (lifecycleGuard != null) lifecycleGuard.requireCloseable(entity);
    return changeStatus(tenantId, userId, stageId, CompetitionStageStatus.IN_PROGRESS,
        CompetitionStageStatus.CLOSED, "close_stage", null);
  }

  @Transactional
  public CompetitionStageEntity markSettled(Long tenantId, Long userId, Long stageId) {
    CompetitionStageEntity entity = locked(tenantId, stageId);
    if (lifecycleGuard != null) lifecycleGuard.requireSettlementReady(tenantId, entity);
    return changeStatus(tenantId, userId, stageId, CompetitionStageStatus.CLOSED,
        CompetitionStageStatus.SETTLED, "mark_settled", null);
  }

  @Transactional
  public CompetitionStageEntity publishResult(Long tenantId, Long userId, Long stageId) {
    CompetitionStageEntity entity = locked(tenantId, stageId);
    if (lifecycleGuard != null) lifecycleGuard.requireResultPublishable(tenantId, entity);
    return changeStatus(tenantId, userId, stageId, CompetitionStageStatus.SETTLED,
        CompetitionStageStatus.RESULT_PUBLISHED, "publish_result", null);
  }

  private CompetitionStageEntity changeStatus(
      Long tenantId, Long userId, Long stageId, CompetitionStageStatus expected,
      CompetitionStageStatus target, String action, String reason) {
    CompetitionStageEntity entity = locked(tenantId, stageId);
    CompetitionStageStatus from = CompetitionStageStatus.fromCode(entity.getStatus());
    if (from != expected) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "赛段当前状态不允许执行" + action);
    }
    lifecycleDomainService.transition(from.getCode(), target);
    LocalDateTime now = LocalDateTime.now();
    entity.setStatus(target.getCode());
    if (target == CompetitionStageStatus.READY_LOCKED) {
      entity.setFrozenAt(now);
      entity.setReadyAt(now);
    } else if (target == CompetitionStageStatus.CLOSED) {
      entity.setClosedAt(now);
    } else if (target == CompetitionStageStatus.RESULT_PUBLISHED) {
      entity.setResultPublishedAt(now);
    }
    entity.setUpdatedBy(userId);
    if (mapper.updateById(entity) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "赛段状态已发生变化，请刷新后重试");
    }
    writeHistory(tenantId, userId, entity, from, target, action, reason);
    return entity;
  }

  private CompetitionStageEntity locked(Long tenantId, Long stageId) {
    if (lifecycleGuard != null) return lifecycleGuard.lockStage(tenantId, stageId);
    return findExisting(tenantId, stageId);
  }

  private void writeHistory(Long tenantId, Long userId, CompetitionStageEntity entity,
      CompetitionStageStatus from, CompetitionStageStatus to, String action, String reason) {
    if (lifecycleHistoryMapper == null) return;
    CompetitionStageLifecycleHistoryEntity history = new CompetitionStageLifecycleHistoryEntity();
    history.setTenantId(tenantId);
    history.setEventId(entity.getEventId());
    history.setStageId(entity.getId());
    history.setFromStatus(from.getCode());
    history.setToStatus(to.getCode());
    history.setAction(action);
    history.setReason(reason);
    history.setCreatedBy(userId);
    history.setUpdatedBy(userId);
    history.setDeleted(0);
    if (lifecycleHistoryMapper.insert(history) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "赛段生命周期历史记录失败");
    }
  }

  private CompetitionStageType validateRequest(
      Long tenantId,
      Long eventId,
      Long excludedStageId,
      String requestedName,
      String requestedType,
      Integer requestedSortOrder,
      LocalDateTime requestedStartAt,
      LocalDateTime requestedEndAt) {
    CompetitionStageType stageType = CompetitionStageType.fromCode(requestedType);
    if (stageType == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛段类型必须是固定预设类型");
    }
    if (requestedName != null && !requestedName.isBlank()
        && !stageType.getDisplayName().equals(requestedName.trim())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛段名称由赛段类型自动生成，不能自由填写");
    }
    if (requestedSortOrder != null && requestedSortOrder != stageType.getSortOrder()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛段顺序由赛段类型自动生成，不能自由填写");
    }
    List<CompetitionStageEntity> existing = mapper.selectList(new LambdaQueryWrapper<CompetitionStageEntity>()
        .eq(CompetitionStageEntity::getTenantId, tenantId)
        .eq(CompetitionStageEntity::getEventId, eventId)
        .isNull(CompetitionStageEntity::getEventWorkTypeId)
        .isNull(CompetitionStageEntity::getEventWorkTypeLevelCode)
        .eq(CompetitionStageEntity::getDeleted, 0));
    if (existing == null) {
      existing = new ArrayList<>();
    }
    List<CompetitionStageEntity> active = existing.stream()
        .filter(stage -> excludedStageId == null || !excludedStageId.equals(stage.getId()))
        .toList();
    if (active.stream().anyMatch(stage -> stageType.getCode().equals(stage.getStageType()))) {
      throw new BusinessException(ErrorCode.CONFLICT, "同一赛事不能重复配置同一预设赛段");
    }
    if (active.size() >= CompetitionStageType.values().length) {
      throw new BusinessException(ErrorCode.CONFLICT, "同一赛事最多配置五个预设赛段");
    }
    validateWindowOrder(active, stageType, requestedStartAt, requestedEndAt);
    return stageType;
  }

  private void validateWindowOrder(
      List<CompetitionStageEntity> existing,
      CompetitionStageType requestedType,
      LocalDateTime requestedStartAt,
      LocalDateTime requestedEndAt) {
    List<StageWindow> windows = new ArrayList<>();
    for (CompetitionStageEntity stage : existing) {
      CompetitionStageType type = CompetitionStageType.fromCode(stage.getStageType());
      if (type != null && stage.getStartAt() != null && stage.getEndAt() != null) {
        windows.add(new StageWindow(type, stage.getStartAt(), stage.getEndAt()));
      }
    }
    if (requestedStartAt != null && requestedEndAt != null) {
      windows.add(new StageWindow(requestedType, requestedStartAt, requestedEndAt));
    }
    windows.sort(java.util.Comparator.comparingInt(window -> window.type().getSortOrder()));
    for (int index = 1; index < windows.size(); index++) {
      StageWindow previous = windows.get(index - 1);
      StageWindow current = windows.get(index);
      if (current.startAt().isBefore(previous.endAt())) {
        throw new BusinessException(ErrorCode.CONFLICT, "赛段时间窗口重叠或未按预设顺序递增");
      }
    }
  }

  private record StageWindow(
      CompetitionStageType type,
      LocalDateTime startAt,
      LocalDateTime endAt) {
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

  private LambdaQueryWrapper<CompetitionStageEntity> stageScopeWrapper(
      Long tenantId,
      Long eventId,
      Long eventWorkTypeId,
      String eventWorkTypeLevelCode) {
    LambdaQueryWrapper<CompetitionStageEntity> wrapper = new LambdaQueryWrapper<CompetitionStageEntity>()
        .eq(CompetitionStageEntity::getTenantId, tenantId)
        .eq(CompetitionStageEntity::getEventId, eventId)
        .eq(CompetitionStageEntity::getDeleted, 0);
    if (eventWorkTypeId == null) {
      wrapper.isNull(CompetitionStageEntity::getEventWorkTypeId)
          .isNull(CompetitionStageEntity::getEventWorkTypeLevelCode);
    } else {
      wrapper.eq(CompetitionStageEntity::getEventWorkTypeId, eventWorkTypeId)
          .eq(CompetitionStageEntity::getEventWorkTypeLevelCode, eventWorkTypeLevelCode);
    }
    return wrapper;
  }

  private String normalizeScopeLevelCode(Long eventWorkTypeId, String eventWorkTypeLevelCode) {
    if (eventWorkTypeId == null) {
      if (eventWorkTypeLevelCode != null && !eventWorkTypeLevelCode.isBlank()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "指定工种等级时必须同时指定赛事工种");
      }
      return null;
    }
    if (eventWorkTypeLevelCode == null
        || !Set.of("1", "2", "3", "4", "5").contains(eventWorkTypeLevelCode.trim())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛事工种等级编码必须是1至5");
    }
    return eventWorkTypeLevelCode.trim();
  }

  private void validateDateRange(LocalDateTime startAt, LocalDateTime endAt) {
    if ((startAt == null) != (endAt == null)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛段开始和结束时间必须同时填写");
    }
    if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛段结束时间不能早于开始时间");
    }
    if (startAt != null && endAt != null && endAt.equals(startAt)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛段结束时间必须晚于开始时间");
    }
  }

  private CompetitionEventEntity loadEventForValidation(Long tenantId, Long eventId) {
    if (eventMapper == null) {
      // The lightweight constructor is retained for isolated unit tests that do not load event data.
      return null;
    }
    CompetitionEventEntity event = eventMapper.selectForUpdate(tenantId, eventId);
    if (event == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事不存在");
    }
    return event;
  }

  private void requireDraftArrangementEvent(CompetitionEventEntity event) {
    com.guoyu.platform.competition.domain.CompetitionEventArrangementGuard.requireOpen(
        event, "已关闭或已归档赛事不允许修改赛段");
  }

  private void validateEventWindow(
      CompetitionEventEntity event,
      LocalDateTime stageStartAt,
      LocalDateTime stageEndAt) {
    if (event == null) return;
    if (event.getStartAt() != null && stageStartAt != null
        && stageStartAt.isBefore(event.getStartAt())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛段开始时间不能早于赛事开始时间");
    }
    if (event.getEndAt() != null && stageEndAt != null
        && stageEndAt.isAfter(event.getEndAt())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛段结束时间不能晚于赛事结束时间");
    }
  }
}
