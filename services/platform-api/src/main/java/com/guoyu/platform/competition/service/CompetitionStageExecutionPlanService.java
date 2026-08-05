package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.domain.CompetitionStageContentProvider;
import com.guoyu.platform.competition.domain.CompetitionStageExecutionPlanDomainService;
import com.guoyu.platform.competition.domain.CompetitionPromotionRuleDomainService;
import com.guoyu.platform.competition.domain.StageAllocationCapability;
import com.guoyu.platform.competition.domain.StageBookingProjectionCapability;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionRegistrationEntity;
import com.guoyu.platform.competition.entity.CompetitionStageAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.entity.CompetitionStageExecutionPlanEntity;
import com.guoyu.platform.competition.entity.CompetitionStagePlanResourceEntity;
import com.guoyu.platform.competition.entity.CompetitionStageResourceBookingEntity;
import com.guoyu.platform.competition.entity.CompetitionStageRosterAdjustmentEntity;
import com.guoyu.platform.competition.entity.CompetitionStageRosterItemEntity;
import com.guoyu.platform.competition.entity.CompetitionStageRosterSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionStagePromotionItemEntity;
import com.guoyu.platform.competition.entity.CompetitionStagePromotionSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeStageEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
import com.guoyu.platform.competition.enums.CompetitionExecutionPlanStatus;
import com.guoyu.platform.competition.enums.CompetitionStageContentType;
import com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageExecutionPlanMapper;
import com.guoyu.platform.competition.mapper.CompetitionStagePlanResourceMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageResourceBookingMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageRosterAdjustmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageRosterItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageRosterSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionStagePromotionItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionStagePromotionSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeStageMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreRuleMapper;
import com.guoyu.platform.competition.dto.CompetitionStagePlanResourceBatchReq;
import com.guoyu.platform.competition.dto.CompetitionStagePlanResourceItemReq;
import com.guoyu.platform.competition.dto.CompetitionStageRosterAdjustmentReq;
import com.guoyu.platform.competition.dto.CompetitionStageRosterGenerateReq;
import com.guoyu.platform.competition.dto.CompetitionStageExecutionPlanCreateReq;
import com.guoyu.platform.competition.vo.CompetitionStagePlanPreflightCheckVO;
import com.guoyu.platform.competition.vo.CompetitionStagePlanPreflightVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionStageExecutionPlanService {

  @Autowired(required = false)
  private CompetitionEventWorkTypeStageMapper workTypeStageMapper;

  private final CompetitionStageExecutionPlanMapper planMapper;
  private final CompetitionStagePlanResourceMapper resourceMapper;
  private final CompetitionStageResourceBookingMapper bookingMapper;
  private final CompetitionStageRosterSnapshotMapper rosterSnapshotMapper;
  private final CompetitionStageRosterItemMapper rosterItemMapper;
  private final CompetitionStageRosterAdjustmentMapper rosterAdjustmentMapper;
  private final CompetitionStageAssignmentMapper assignmentMapper;
  private final CompetitionContestantProfileMapper profileMapper;
  private final CompetitionEventMemberMapper memberMapper;
  private final CompetitionRegistrationMapper registrationMapper;
  private final CompetitionStagePromotionSnapshotMapper promotionSnapshotMapper;
  private final CompetitionStagePromotionItemMapper promotionItemMapper;
  private final CompetitionStageMapper stageMapper;
  private final CompetitionScoreRuleMapper scoreRuleMapper;
  private final CompetitionStageContentService contentService;
  private final CompetitionStageExecutionPlanDomainService domainService;
  private final Map<String, CompetitionStageContentProvider> providers;

  public CompetitionStageExecutionPlanService(
      CompetitionStageExecutionPlanMapper planMapper,
      CompetitionStagePlanResourceMapper resourceMapper,
      CompetitionStageResourceBookingMapper bookingMapper,
      CompetitionStageRosterSnapshotMapper rosterSnapshotMapper,
      CompetitionStageRosterItemMapper rosterItemMapper,
      CompetitionStageRosterAdjustmentMapper rosterAdjustmentMapper,
      CompetitionStageAssignmentMapper assignmentMapper,
      CompetitionContestantProfileMapper profileMapper,
      CompetitionEventMemberMapper memberMapper,
      CompetitionRegistrationMapper registrationMapper,
      CompetitionStageContentService contentService,
      List<CompetitionStageContentProvider> providers,
      CompetitionStagePromotionSnapshotMapper promotionSnapshotMapper,
      CompetitionStagePromotionItemMapper promotionItemMapper,
      CompetitionStageMapper stageMapper,
      CompetitionScoreRuleMapper scoreRuleMapper) {
    this.planMapper = planMapper;
    this.resourceMapper = resourceMapper;
    this.bookingMapper = bookingMapper;
    this.rosterSnapshotMapper = rosterSnapshotMapper;
    this.rosterItemMapper = rosterItemMapper;
    this.rosterAdjustmentMapper = rosterAdjustmentMapper;
    this.assignmentMapper = assignmentMapper;
    this.profileMapper = profileMapper;
    this.memberMapper = memberMapper;
    this.registrationMapper = registrationMapper;
    this.promotionSnapshotMapper = promotionSnapshotMapper;
    this.promotionItemMapper = promotionItemMapper;
    this.stageMapper = stageMapper;
    this.scoreRuleMapper = scoreRuleMapper;
    this.contentService = contentService;
    this.domainService = new CompetitionStageExecutionPlanDomainService();
    this.providers = (providers == null ? List.<CompetitionStageContentProvider>of() : providers)
        .stream().filter(Objects::nonNull)
        .collect(Collectors.toUnmodifiableMap(CompetitionStageContentProvider::contentType,
            Function.identity(), (left, right) -> left));
  }

  @Transactional(readOnly = true)
  public List<CompetitionStageExecutionPlanEntity> list(
      Long tenantId, Long stageId, Long eventWorkTypeId, Long levelId) {
    List<CompetitionStageExecutionPlanEntity> records = planMapper.selectList(
        new LambdaQueryWrapper<CompetitionStageExecutionPlanEntity>()
            .eq(CompetitionStageExecutionPlanEntity::getTenantId, tenantId)
            .eq(CompetitionStageExecutionPlanEntity::getStageId, stageId)
            .eq(CompetitionStageExecutionPlanEntity::getEventWorkTypeId, eventWorkTypeId)
            .eq(levelId != null, CompetitionStageExecutionPlanEntity::getEventWorkTypeLevelId, levelId)
            .eq(CompetitionStageExecutionPlanEntity::getDeleted, 0)
            .orderByDesc(CompetitionStageExecutionPlanEntity::getPlanVersion));
    return records == null ? new ArrayList<>() : new ArrayList<>(records);
  }

  @Transactional(readOnly = true)
  public CompetitionStageExecutionPlanEntity get(Long tenantId, Long planId) {
    CompetitionStageExecutionPlanEntity plan = planMapper.selectOne(
        new LambdaQueryWrapper<CompetitionStageExecutionPlanEntity>()
            .eq(CompetitionStageExecutionPlanEntity::getTenantId, tenantId)
            .eq(CompetitionStageExecutionPlanEntity::getId, planId)
            .eq(CompetitionStageExecutionPlanEntity::getDeleted, 0));
    if (plan == null) throw new BusinessException(ErrorCode.NOT_FOUND, "执行方案不存在");
    return plan;
  }

  @Transactional
  public CompetitionStageExecutionPlanEntity create(
      Long tenantId, Long userId, CompetitionStageEntity stage,
      CompetitionStageExecutionPlanCreateReq req) {
    List<CompetitionStageExecutionPlanEntity> existing = list(
        tenantId, stage.getId(), req.eventWorkTypeId(), req.eventWorkTypeLevelId());
    int version = existing.stream().map(CompetitionStageExecutionPlanEntity::getPlanVersion)
        .filter(Objects::nonNull).max(Integer::compareTo).orElse(0) + 1;
    CompetitionStageExecutionPlanEntity plan = new CompetitionStageExecutionPlanEntity();
    plan.setTenantId(tenantId);
    plan.setEventId(stage.getEventId());
    plan.setStageId(stage.getId());
    plan.setEventWorkTypeId(req.eventWorkTypeId());
    plan.setEventWorkTypeLevelId(req.eventWorkTypeLevelId());
    plan.setPlanVersion(version);
    plan.setStatus(CompetitionExecutionPlanStatus.DRAFT.getCode());
    plan.setPreflightStatus("pending");
    plan.setResourceVersion(req.resourceVersion());
    plan.setScoreRuleId(req.scoreRuleId());
    CompetitionEventWorkTypeStageEntity configuredStageScope = workTypeStageMapper == null ? null
        : workTypeStageMapper.selectOne(new LambdaQueryWrapper<CompetitionEventWorkTypeStageEntity>()
            .eq(CompetitionEventWorkTypeStageEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeStageEntity::getEventId, stage.getEventId())
            .eq(CompetitionEventWorkTypeStageEntity::getEventWorkTypeId, req.eventWorkTypeId())
            .eq(CompetitionEventWorkTypeStageEntity::getStageId, stage.getId())
            .eq(CompetitionEventWorkTypeStageEntity::getDeleted, 0));
    if (configuredStageScope != null && !Objects.equals(configuredStageScope.getEnabled(), 1)) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前工种未启用该赛段");
    }
    CompetitionEventWorkTypeStageEntity stageScope = configuredStageScope;
    Integer promotionQuota = req.promotionQuota() != null ? req.promotionQuota()
        : stageScope == null ? null : stageScope.getPromotionQuota();
    Integer promotionPercentage = req.promotionPercentage() != null ? req.promotionPercentage()
        : stageScope == null ? null : stageScope.getPromotionPercentage();
    String promotionRuleType = req.promotionRuleType() != null ? req.promotionRuleType()
        : stageScope == null ? null : stageScope.getPromotionRuleType();
    plan.setPromotionQuota(promotionQuota);
    plan.setPromotionPercentage(promotionPercentage);
    CompetitionPromotionRuleDomainService promotionRules = new CompetitionPromotionRuleDomainService();
    CompetitionPromotionRuleDomainService.ResolvedRule resolvedRule = promotionRules.resolve(
        promotionRuleType, promotionQuota, promotionPercentage);
    plan.setPromotionRuleType(resolvedRule.type());
    plan.setLegacyRule(resolvedRule.legacy() ? 1 : 0);
    plan.setCreatedBy(userId);
    plan.setUpdatedBy(userId);
    plan.setDeleted(0);
    if (planMapper.insert(plan) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "执行方案创建失败");
    }
    return plan;
  }

  @Transactional
  public List<CompetitionStagePlanResourceEntity> replaceResources(
      Long tenantId, Long userId, CompetitionStageEntity stage,
      CompetitionStageExecutionPlanEntity plan, CompetitionStagePlanResourceBatchReq req) {
    domainService.requireEditable(plan.getStatus());
    if (req == null || req.resources() == null || req.resources().isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "执行方案至少需要一项资源引用");
    }
    List<CompetitionStagePlanResourceEntity> old = resources(tenantId, plan.getId());
    List<CompetitionStageResourceBookingEntity> oldBookings = bookings(tenantId, plan.getId());
    for (CompetitionStageResourceBookingEntity booking : oldBookings) {
      booking.setStatus("released");
      booking.setUpdatedBy(userId);
      bookingMapper.updateById(booking);
    }
    for (CompetitionStagePlanResourceEntity resource : old) resourceMapper.deleteById(resource.getId());

    List<CompetitionStagePlanResourceEntity> result = new ArrayList<>();
    Set<String> keys = new HashSet<>();
    for (CompetitionStagePlanResourceItemReq item : req.resources()) {
      validateResource(stage, item);
      String key = item.contentType() + ":" + item.resourceType() + ":"
          + String.valueOf(item.resourceId()) + ":" + String.valueOf(item.resourceCode());
      if (!keys.add(key)) throw new BusinessException(ErrorCode.CONFLICT, "执行方案存在重复资源引用");
      CompetitionStagePlanResourceEntity resource = new CompetitionStagePlanResourceEntity();
      resource.setTenantId(tenantId);
      resource.setPlanId(plan.getId());
      resource.setEventId(stage.getEventId());
      resource.setStageId(stage.getId());
      resource.setContentType(item.contentType());
      resource.setResourceType(item.resourceType());
      resource.setResourceId(item.resourceId());
      resource.setResourceCode(item.resourceCode());
      resource.setResourceVersion(item.resourceVersion());
      resource.setStartAt(item.startAt());
      resource.setEndAt(item.endAt());
      resource.setResourcePayload(item.resourcePayload());
      resource.setStatus(item.status() == null || item.status().isBlank() ? "enabled" : item.status());
      resource.setCreatedBy(userId);
      resource.setUpdatedBy(userId);
      resource.setDeleted(0);
      if (resourceMapper.insert(resource) != 1) throw new BusinessException(ErrorCode.CONFLICT, "资源引用保存失败");
      if ("enabled".equals(resource.getStatus()) && resource.getResourceId() != null
          && resource.getStartAt() != null && resource.getEndAt() != null) {
        ensureBookingAvailable(tenantId, resource, plan.getId());
        CompetitionStageResourceBookingEntity booking = new CompetitionStageResourceBookingEntity();
        booking.setTenantId(tenantId);
        booking.setPlanId(plan.getId());
        booking.setEventId(stage.getEventId());
        booking.setStageId(stage.getId());
        booking.setResourceType(resource.getResourceType());
        booking.setResourceId(resource.getResourceId());
        booking.setStartAt(resource.getStartAt());
        booking.setEndAt(resource.getEndAt());
        booking.setStatus("reserved");
        booking.setCreatedBy(userId);
        booking.setUpdatedBy(userId);
        booking.setDeleted(0);
        if (bookingMapper.insert(booking) != 1) throw new BusinessException(ErrorCode.CONFLICT, "资源预订保存失败");
      }
      result.add(resource);
    }
    plan.setResourceVersion("plan-" + plan.getPlanVersion() + "-" + LocalDateTime.now());
    plan.setPreflightStatus("pending");
    plan.setUpdatedBy(userId);
    planMapper.updateById(plan);
    return result;
  }

  @Transactional(readOnly = true)
  public List<CompetitionStagePlanResourceEntity> resources(Long tenantId, Long planId) {
    List<CompetitionStagePlanResourceEntity> records = resourceMapper.selectList(
        new LambdaQueryWrapper<CompetitionStagePlanResourceEntity>()
            .eq(CompetitionStagePlanResourceEntity::getTenantId, tenantId)
            .eq(CompetitionStagePlanResourceEntity::getPlanId, planId)
            .eq(CompetitionStagePlanResourceEntity::getDeleted, 0)
            .orderByAsc(CompetitionStagePlanResourceEntity::getContentType)
            .orderByAsc(CompetitionStagePlanResourceEntity::getId));
    return records == null ? new ArrayList<>() : new ArrayList<>(records);
  }

  @Transactional(readOnly = true)
  public CompetitionStagePlanPreflightVO preflight(
      Long tenantId, CompetitionStageExecutionPlanEntity plan, CompetitionStageEntity stage) {
    List<CompetitionStagePlanPreflightCheckVO> checks = new ArrayList<>();
    checks.add(check("PLAN_DRAFT", CompetitionExecutionPlanStatus.DRAFT.getCode().equals(plan.getStatus()),
        "只有草稿方案可进入发布预检"));
    List<CompetitionStagePlanResourceEntity> resources = resources(tenantId, plan.getId()).stream()
        .filter(item -> "enabled".equals(item.getStatus())).toList();
    List<String> enabledContents = contentService.list(tenantId, stage.getId()).stream()
        .filter(item -> "enabled".equals(item.getStatus()))
        .map(item -> item.getContentType()).toList();
    for (String content : enabledContents) {
      boolean present = resources.stream().anyMatch(item -> content.equals(item.getContentType()));
      checks.add(check("RESOURCE_" + content.toUpperCase(), present,
          present ? "已配置内容资源" : "缺少启用内容的资源引用"));
      CompetitionStageContentProvider provider = providers.get(content);
      if (provider != null && present) {
        List<String> providerChecks;
        try {
          providerChecks = provider.preflight(resources.stream()
              .filter(item -> content.equals(item.getContentType())).toList());
          providerChecks = new ArrayList<>(providerChecks == null ? List.of() : providerChecks);
          List<CompetitionStagePlanResourceEntity> contentResources = resources.stream()
              .filter(item -> content.equals(item.getContentType())).toList();
          if (provider instanceof StageAllocationCapability allocationCapability) {
            providerChecks.addAll(allocationCapability.confirmAllocation(contentResources));
          }
          if (provider instanceof StageBookingProjectionCapability bookingCapability) {
            long expectedBookings = contentResources.stream()
                .filter(item -> item.getResourceId() != null && item.getStartAt() != null && item.getEndAt() != null)
                .count();
            boolean bookingProjectionReady = bookingCapability.projectBookings(contentResources).size() == expectedBookings;
            if (!bookingProjectionReady) providerChecks.add("BOOKING_PROJECTION_INVALID");
          }
        } catch (RuntimeException ex) {
          log.warn("Stage content provider preflight failed, contentType={}, planId={}", content, plan.getId(), ex);
          providerChecks = List.of("PROVIDER_ERROR");
        }
        for (String code : providerChecks == null ? List.<String>of() : providerChecks) {
          checks.add(check(code, false, "内容 provider 尚未就绪"));
        }
      }
    }
    List<CompetitionStageRosterSnapshotEntity> rosters = rosterSnapshots(tenantId, plan.getId());
    boolean rosterReady = rosters.stream().anyMatch(item -> "published".equals(item.getStatus()));
    checks.add(check("ROSTER_NOT_PUBLISHED", rosterReady,
        rosterReady ? "名单快照已发布" : "名单快照尚未发布"));
    boolean timeReady = resources.stream().allMatch(item -> item.getStartAt() == null
        || item.getEndAt() == null || withinStage(item.getStartAt(), item.getEndAt(), stage));
    checks.add(check("RESOURCE_OUTSIDE_STAGE", timeReady,
        timeReady ? "资源时间落在赛段窗口内" : "资源时间超出赛段窗口"));
    boolean ready = checks.stream().allMatch(CompetitionStagePlanPreflightCheckVO::passed);
    return new CompetitionStagePlanPreflightVO(plan.getId(), ready, checks);
  }

  @Transactional
  public CompetitionStageExecutionPlanEntity publish(
      Long tenantId, Long userId, CompetitionStageExecutionPlanEntity plan,
      CompetitionStageEntity stage) {
    domainService.requirePublishable(plan.getStatus());
    if (!"configuring".equals(stage.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "执行方案只能在配置中的赛段发布");
    }
    CompetitionStagePlanPreflightVO preflight = preflight(tenantId, plan, stage);
    plan.setPreflightStatus(preflight.readyToPublish() ? "passed" : "blocked");
    if (!preflight.readyToPublish()) throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "执行方案预检未通过");
    freezeExecutionScope(tenantId, plan, stage);
    CompetitionStageExecutionPlanEntity old = planMapper.selectOne(
        new LambdaQueryWrapper<CompetitionStageExecutionPlanEntity>()
            .eq(CompetitionStageExecutionPlanEntity::getTenantId, tenantId)
            .eq(CompetitionStageExecutionPlanEntity::getStageId, plan.getStageId())
            .eq(CompetitionStageExecutionPlanEntity::getEventWorkTypeId, plan.getEventWorkTypeId())
            .eq(CompetitionStageExecutionPlanEntity::getEventWorkTypeLevelId, plan.getEventWorkTypeLevelId())
            .eq(CompetitionStageExecutionPlanEntity::getStatus, "published")
            .eq(CompetitionStageExecutionPlanEntity::getDeleted, 0));
    if (old != null && !Objects.equals(old.getId(), plan.getId())) {
      old.setStatus("withdrawn");
      old.setWithdrawnAt(LocalDateTime.now());
      old.setWithdrawnBy(userId);
      old.setWithdrawReason("replaced_by_new_plan");
      old.setUpdatedBy(userId);
      planMapper.updateById(old);
    }
    plan.setStatus("published");
    plan.setPublishedAt(LocalDateTime.now());
    plan.setPublishedBy(userId);
    plan.setUpdatedBy(userId);
    if (planMapper.updateById(plan) != 1) throw new BusinessException(ErrorCode.CONFLICT, "执行方案发布状态已变化");
    return plan;
  }

  @Transactional
  public CompetitionStageExecutionPlanEntity withdraw(
      Long tenantId, Long userId, CompetitionStageExecutionPlanEntity plan, String reason) {
    domainService.requireWithdrawable(plan.getStatus());
    plan.setStatus("withdrawn");
    plan.setWithdrawnAt(LocalDateTime.now());
    plan.setWithdrawnBy(userId);
    plan.setWithdrawReason(reason);
    plan.setUpdatedBy(userId);
    if (planMapper.updateById(plan) != 1) throw new BusinessException(ErrorCode.CONFLICT, "执行方案撤回状态已变化");
    return plan;
  }

  @Transactional
  public CompetitionStageRosterSnapshotEntity generateRoster(
      Long tenantId, Long userId, CompetitionStageExecutionPlanEntity plan,
      CompetitionStageRosterGenerateReq req) {
    domainService.requireEditable(plan.getStatus());
    if (!List.of("eligibility", "promotion", "manual").contains(req.sourceType())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "名单来源类型无效");
    }
    CompetitionStageEntity stage = currentStage(tenantId, plan.getStageId());
    CompetitionStagePromotionSnapshotEntity promotionSource = null;
    if ("manual".equals(req.sourceType()) && previousStage(tenantId, stage) != null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "后续赛段名单只能来自直接前置赛段的已发布晋级快照");
    }
    if ("promotion".equals(req.sourceType())) {
      promotionSource = directPromotionSource(tenantId, plan, stage);
      final CompetitionStagePromotionSnapshotEntity sourceSnapshot = promotionSource;
      CompetitionStageRosterSnapshotEntity existing = rosterSnapshots(tenantId, plan.getId()).stream()
          .filter(item -> "promotion".equals(item.getSourceType())
              && Objects.equals(item.getSourcePromotionSnapshotId(), sourceSnapshot.getId()))
          .findFirst().orElse(null);
      if (existing != null) return existing;
    }
    int version = rosterSnapshots(tenantId, plan.getId()).stream()
        .map(CompetitionStageRosterSnapshotEntity::getSnapshotVersion).filter(Objects::nonNull)
        .max(Integer::compareTo).orElse(0) + 1;
    CompetitionStageRosterSnapshotEntity snapshot = new CompetitionStageRosterSnapshotEntity();
    snapshot.setTenantId(tenantId);
    snapshot.setPlanId(plan.getId());
    snapshot.setEventId(plan.getEventId());
    snapshot.setStageId(plan.getStageId());
    snapshot.setEventWorkTypeId(plan.getEventWorkTypeId());
    snapshot.setEventWorkTypeLevelId(plan.getEventWorkTypeLevelId());
    snapshot.setSnapshotVersion(version);
    snapshot.setSourceType(req.sourceType());
    snapshot.setSourcePromotionSnapshotId(promotionSource == null ? null : promotionSource.getId());
    snapshot.setStatus("draft");
    snapshot.setGeneratedAt(LocalDateTime.now());
    snapshot.setCreatedBy(userId);
    snapshot.setUpdatedBy(userId);
    snapshot.setDeleted(0);
    if (rosterSnapshotMapper.insert(snapshot) != 1) throw new BusinessException(ErrorCode.CONFLICT, "名单快照创建失败");

    List<CompetitionContestantProfileEntity> profiles = profileMapper.selectList(
        new LambdaQueryWrapper<CompetitionContestantProfileEntity>()
            .eq(CompetitionContestantProfileEntity::getTenantId, tenantId)
            .eq(CompetitionContestantProfileEntity::getEventWorkTypeId, plan.getEventWorkTypeId())
            .eq(plan.getEventWorkTypeLevelId() != null,
                CompetitionContestantProfileEntity::getEventWorkTypeLevelId, plan.getEventWorkTypeLevelId())
            .eq(CompetitionContestantProfileEntity::getWorkTypeMatchStatus, "matched")
            .eq(CompetitionContestantProfileEntity::getDeleted, 0));
    if (profiles == null) profiles = Collections.emptyList();
    Set<Long> memberIds = profiles.stream().map(CompetitionContestantProfileEntity::getMemberId)
        .filter(Objects::nonNull).collect(Collectors.toSet());
    Map<Long, CompetitionEventMemberEntity> members = new HashMap<>();
    if (!memberIds.isEmpty()) {
      List<CompetitionEventMemberEntity> rows = memberMapper.selectList(
          new LambdaQueryWrapper<CompetitionEventMemberEntity>()
              .eq(CompetitionEventMemberEntity::getTenantId, tenantId)
              .eq(CompetitionEventMemberEntity::getEventId, plan.getEventId())
              .in(CompetitionEventMemberEntity::getId, memberIds)
              .eq(CompetitionEventMemberEntity::getRoleType, "contestant")
              .eq(CompetitionEventMemberEntity::getStatus, "active")
              .eq(CompetitionEventMemberEntity::getDeleted, 0));
      if (rows != null) rows.forEach(row -> members.put(row.getId(), row));
    }
    Set<Long> registrationIds = profiles.stream().map(CompetitionContestantProfileEntity::getRegistrationId)
        .filter(Objects::nonNull).collect(Collectors.toSet());
    Map<Long, CompetitionRegistrationEntity> registrations = new HashMap<>();
    if (!registrationIds.isEmpty()) {
      List<CompetitionRegistrationEntity> rows = registrationMapper.selectList(
          new LambdaQueryWrapper<CompetitionRegistrationEntity>()
              .eq(CompetitionRegistrationEntity::getTenantId, tenantId)
              .eq(CompetitionRegistrationEntity::getEventId, plan.getEventId())
              .in(CompetitionRegistrationEntity::getId, registrationIds)
              .in(CompetitionRegistrationEntity::getStatus, List.of("eligible_unactivated", "eligible_activated"))
              .eq(CompetitionRegistrationEntity::getDeleted, 0));
      if (rows != null) rows.forEach(row -> registrations.put(row.getId(), row));
    }
    if (promotionSource != null) {
      profiles = new ArrayList<>();
      List<CompetitionStagePromotionItemEntity> selected = promotionItemMapper.selectList(
          new LambdaQueryWrapper<CompetitionStagePromotionItemEntity>()
              .eq(CompetitionStagePromotionItemEntity::getTenantId, tenantId)
              .eq(CompetitionStagePromotionItemEntity::getPromotionSnapshotId, promotionSource.getId())
              .eq(CompetitionStagePromotionItemEntity::getSelectionStatus, "selected")
              .eq(CompetitionStagePromotionItemEntity::getDeleted, 0)
              .orderByAsc(CompetitionStagePromotionItemEntity::getSourceRank));
      if (selected == null || selected.isEmpty()) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "晋级快照没有有效晋级选手");
      }
      Set<Long> selectedMembers = selected.stream().map(CompetitionStagePromotionItemEntity::getContestantMemberId)
          .filter(Objects::nonNull).collect(Collectors.toSet());
      List<CompetitionContestantProfileEntity> selectedProfiles = profileMapper.selectList(
          new LambdaQueryWrapper<CompetitionContestantProfileEntity>()
              .eq(CompetitionContestantProfileEntity::getTenantId, tenantId)
              .in(CompetitionContestantProfileEntity::getMemberId, selectedMembers)
              .eq(CompetitionContestantProfileEntity::getEventWorkTypeId, plan.getEventWorkTypeId())
              .eq(plan.getEventWorkTypeLevelId() != null,
                  CompetitionContestantProfileEntity::getEventWorkTypeLevelId, plan.getEventWorkTypeLevelId())
              .eq(CompetitionContestantProfileEntity::getDeleted, 0));
      if (selectedProfiles != null) profiles.addAll(selectedProfiles);
    }
    for (CompetitionContestantProfileEntity profile : profiles) {
      if (!members.containsKey(profile.getMemberId()) || !registrations.containsKey(profile.getRegistrationId())) continue;
      Long sourcePromotionItemId = null;
      if (promotionSource != null) {
        CompetitionStagePromotionItemEntity sourceItem = promotionItemMapper.selectOne(new LambdaQueryWrapper<CompetitionStagePromotionItemEntity>()
            .eq(CompetitionStagePromotionItemEntity::getTenantId, tenantId)
            .eq(CompetitionStagePromotionItemEntity::getPromotionSnapshotId, promotionSource.getId())
            .eq(CompetitionStagePromotionItemEntity::getContestantMemberId, profile.getMemberId())
            .eq(CompetitionStagePromotionItemEntity::getSelectionStatus, "selected")
            .eq(CompetitionStagePromotionItemEntity::getDeleted, 0));
        sourcePromotionItemId = sourceItem == null ? null : sourceItem.getId();
      }
      CompetitionStageRosterItemEntity item = new CompetitionStageRosterItemEntity();
      item.setTenantId(tenantId);
      item.setSnapshotId(snapshot.getId());
      item.setEventId(plan.getEventId());
      item.setStageId(plan.getStageId());
      item.setEventWorkTypeId(plan.getEventWorkTypeId());
      item.setEventWorkTypeLevelId(plan.getEventWorkTypeLevelId());
      item.setContestantMemberId(profile.getMemberId());
      item.setContestantProfileId(profile.getId());
      item.setRegistrationId(profile.getRegistrationId());
      item.setInclusionStatus("included");
      item.setEligibilityStatus("eligible");
      item.setSourcePromotionItemId(sourcePromotionItemId);
      item.setCreatedBy(userId);
      item.setUpdatedBy(userId);
      item.setDeleted(0);
      rosterItemMapper.insert(item);
    }
    return snapshot;
  }

  @Transactional
  public CompetitionStageRosterSnapshotEntity publishRoster(
      Long tenantId, Long userId, CompetitionStageExecutionPlanEntity plan, Long snapshotId) {
    domainService.requireEditable(plan.getStatus());
    CompetitionStageRosterSnapshotEntity snapshot = rosterSnapshot(tenantId, snapshotId);
    if (!Objects.equals(snapshot.getPlanId(), plan.getId())) throw new BusinessException(ErrorCode.NOT_FOUND, "名单快照不属于当前方案");
    List<CompetitionStageRosterItemEntity> items = rosterItems(tenantId, snapshotId);
    if (items.stream().noneMatch(item -> "included".equals(item.getInclusionStatus()))) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "名单快照没有可用选手");
    }
    for (CompetitionStageRosterSnapshotEntity old : rosterSnapshots(tenantId, plan.getId())) {
      if ("published".equals(old.getStatus()) && !Objects.equals(old.getId(), snapshotId)) {
        old.setStatus("superseded"); old.setUpdatedBy(userId); rosterSnapshotMapper.updateById(old);
      }
    }
    snapshot.setStatus("published");
    snapshot.setPublishedAt(LocalDateTime.now());
    snapshot.setPublishedBy(userId);
    snapshot.setUpdatedBy(userId);
    rosterSnapshotMapper.updateById(snapshot);
    plan.setRosterSnapshotId(snapshotId); plan.setUpdatedBy(userId); planMapper.updateById(plan);
    for (CompetitionStageRosterItemEntity item : items) {
      if (!"included".equals(item.getInclusionStatus())) continue;
      CompetitionContestantProfileEntity profile = profileMapper.selectForUpdateByTenantAndMember(
          tenantId, item.getContestantMemberId());
      if (profile != null && Objects.equals(profile.getRegistrationId(), item.getRegistrationId())) {
        profile.setCurrentStageId(plan.getStageId());
        profile.setPromotionStatus("pending");
        profile.setUpdatedBy(userId);
        profileMapper.updateById(profile);
      }
    }
    return snapshot;
  }

  @Transactional
  public CompetitionStageRosterItemEntity adjustRoster(
      Long tenantId, Long userId, CompetitionStageExecutionPlanEntity plan,
      Long snapshotId, CompetitionStageRosterAdjustmentReq req) {
    domainService.requireEditable(plan.getStatus());
    CompetitionStageRosterSnapshotEntity snapshot = rosterSnapshot(tenantId, snapshotId);
    if (!Objects.equals(snapshot.getPlanId(), plan.getId()) || "published".equals(snapshot.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "已发布名单不可直接调整");
    }
    CompetitionStageRosterItemEntity item = rosterItemMapper.selectOne(
        new LambdaQueryWrapper<CompetitionStageRosterItemEntity>()
            .eq(CompetitionStageRosterItemEntity::getTenantId, tenantId)
            .eq(CompetitionStageRosterItemEntity::getSnapshotId, snapshotId)
            .eq(CompetitionStageRosterItemEntity::getContestantMemberId, req.contestantMemberId())
            .eq(CompetitionStageRosterItemEntity::getDeleted, 0));
    if (item == null) throw new BusinessException(ErrorCode.NOT_FOUND, "名单选手不存在");
    String before = item.getInclusionStatus();
    if (!List.of("included", "removed").contains(req.afterStatus())) throw new BusinessException(ErrorCode.BAD_REQUEST, "名单调整状态无效");
    item.setInclusionStatus(req.afterStatus()); item.setReason(req.reason()); item.setUpdatedBy(userId);
    rosterItemMapper.updateById(item);
    CompetitionStageRosterAdjustmentEntity adjustment = new CompetitionStageRosterAdjustmentEntity();
    adjustment.setTenantId(tenantId); adjustment.setSnapshotId(snapshotId); adjustment.setContestantMemberId(req.contestantMemberId());
    adjustment.setBeforeStatus(before); adjustment.setAfterStatus(req.afterStatus()); adjustment.setReason(req.reason());
    adjustment.setOperatedBy(userId); adjustment.setOperatedAt(LocalDateTime.now()); adjustment.setCreatedBy(userId); adjustment.setUpdatedBy(userId); adjustment.setDeleted(0);
    rosterAdjustmentMapper.insert(adjustment);
    return item;
  }

  @Transactional(readOnly = true)
  public List<CompetitionStageRosterSnapshotEntity> rosterSnapshots(Long tenantId, Long planId) {
    List<CompetitionStageRosterSnapshotEntity> rows = rosterSnapshotMapper.selectList(
        new LambdaQueryWrapper<CompetitionStageRosterSnapshotEntity>()
            .eq(CompetitionStageRosterSnapshotEntity::getTenantId, tenantId)
            .eq(CompetitionStageRosterSnapshotEntity::getPlanId, planId)
            .eq(CompetitionStageRosterSnapshotEntity::getDeleted, 0)
            .orderByDesc(CompetitionStageRosterSnapshotEntity::getSnapshotVersion));
    return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
  }

  @Transactional(readOnly = true)
  public List<CompetitionStageRosterItemEntity> rosterItems(Long tenantId, Long snapshotId) {
    List<CompetitionStageRosterItemEntity> rows = rosterItemMapper.selectList(
        new LambdaQueryWrapper<CompetitionStageRosterItemEntity>()
            .eq(CompetitionStageRosterItemEntity::getTenantId, tenantId)
            .eq(CompetitionStageRosterItemEntity::getSnapshotId, snapshotId)
            .eq(CompetitionStageRosterItemEntity::getDeleted, 0)
            .orderByAsc(CompetitionStageRosterItemEntity::getId));
    return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
  }

  private CompetitionStageRosterSnapshotEntity rosterSnapshot(Long tenantId, Long snapshotId) {
    CompetitionStageRosterSnapshotEntity row = rosterSnapshotMapper.selectOne(
        new LambdaQueryWrapper<CompetitionStageRosterSnapshotEntity>()
            .eq(CompetitionStageRosterSnapshotEntity::getTenantId, tenantId)
            .eq(CompetitionStageRosterSnapshotEntity::getId, snapshotId)
            .eq(CompetitionStageRosterSnapshotEntity::getDeleted, 0));
    if (row == null) throw new BusinessException(ErrorCode.NOT_FOUND, "名单快照不存在");
    return row;
  }

  private CompetitionStageEntity currentStage(Long tenantId, Long stageId) {
    CompetitionStageEntity stage = stageMapper.selectOne(new LambdaQueryWrapper<CompetitionStageEntity>()
        .eq(CompetitionStageEntity::getTenantId, tenantId)
        .eq(CompetitionStageEntity::getId, stageId)
        .eq(CompetitionStageEntity::getDeleted, 0));
    if (stage == null) throw new BusinessException(ErrorCode.NOT_FOUND, "赛段不存在");
    return stage;
  }

  private CompetitionStagePromotionSnapshotEntity directPromotionSource(
      Long tenantId, CompetitionStageExecutionPlanEntity plan, CompetitionStageEntity target) {
    CompetitionStageEntity previous = previousStage(tenantId, target);
    if (previous == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "当前赛段没有直接前置赛段");
    }
    List<CompetitionStagePromotionSnapshotEntity> snapshots = promotionSnapshotMapper.selectList(
        new LambdaQueryWrapper<CompetitionStagePromotionSnapshotEntity>()
            .eq(CompetitionStagePromotionSnapshotEntity::getTenantId, tenantId)
            .eq(CompetitionStagePromotionSnapshotEntity::getEventId, target.getEventId())
            .eq(CompetitionStagePromotionSnapshotEntity::getSourceStageId, previous.getId())
            .eq(CompetitionStagePromotionSnapshotEntity::getTargetStageId, target.getId())
            .eq(CompetitionStagePromotionSnapshotEntity::getEventWorkTypeId, plan.getEventWorkTypeId())
            .eq(plan.getEventWorkTypeLevelId() != null,
                CompetitionStagePromotionSnapshotEntity::getEventWorkTypeLevelId, plan.getEventWorkTypeLevelId())
            .isNull(plan.getEventWorkTypeLevelId() == null,
                CompetitionStagePromotionSnapshotEntity::getEventWorkTypeLevelId)
            .eq(CompetitionStagePromotionSnapshotEntity::getStatus, "published")
            .eq(CompetitionStagePromotionSnapshotEntity::getDeleted, 0)
            .orderByDesc(CompetitionStagePromotionSnapshotEntity::getPublishedAt));
    if (snapshots == null || snapshots.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "直接前置赛段尚未发布有效晋级快照");
    }
    return snapshots.get(0);
  }

  private CompetitionStageEntity previousStage(Long tenantId, CompetitionStageEntity target) {
    List<CompetitionStageEntity> stages = stageMapper.selectList(new LambdaQueryWrapper<CompetitionStageEntity>()
        .eq(CompetitionStageEntity::getTenantId, tenantId)
        .eq(CompetitionStageEntity::getEventId, target.getEventId())
        .eq(CompetitionStageEntity::getDeleted, 0)
        .orderByAsc(CompetitionStageEntity::getSortOrder));
    return stages == null ? null : stages.stream()
        .filter(stage -> stage.getSortOrder() != null && target.getSortOrder() != null
            && stage.getSortOrder() < target.getSortOrder())
        .max(java.util.Comparator.comparing(CompetitionStageEntity::getSortOrder)).orElse(null);
  }

  private void freezeExecutionScope(Long tenantId, CompetitionStageExecutionPlanEntity plan,
      CompetitionStageEntity stage) {
    if (workTypeStageMapper != null) {
      CompetitionEventWorkTypeStageEntity configuredStageScope = workTypeStageMapper.selectOne(
          new LambdaQueryWrapper<CompetitionEventWorkTypeStageEntity>()
              .eq(CompetitionEventWorkTypeStageEntity::getTenantId, tenantId)
              .eq(CompetitionEventWorkTypeStageEntity::getEventId, stage.getEventId())
              .eq(CompetitionEventWorkTypeStageEntity::getEventWorkTypeId, plan.getEventWorkTypeId())
              .eq(CompetitionEventWorkTypeStageEntity::getStageId, stage.getId())
              .eq(CompetitionEventWorkTypeStageEntity::getDeleted, 0));
      if (configuredStageScope != null && !Objects.equals(configuredStageScope.getEnabled(), 1)) {
        throw new BusinessException(ErrorCode.CONFLICT, "当前工种未启用该赛段");
      }
    }
    CompetitionScoreRuleEntity rule;
    if (plan.getScoreRuleId() == null) {
      List<CompetitionScoreRuleEntity> rules = scoreRuleMapper.selectList(new LambdaQueryWrapper<CompetitionScoreRuleEntity>()
          .eq(CompetitionScoreRuleEntity::getTenantId, tenantId)
          .eq(CompetitionScoreRuleEntity::getEventId, stage.getEventId())
          .eq(CompetitionScoreRuleEntity::getStageId, stage.getId())
          .eq(CompetitionScoreRuleEntity::getEventWorkTypeId, plan.getEventWorkTypeId())
          .eq(plan.getEventWorkTypeLevelId() != null,
              CompetitionScoreRuleEntity::getEventWorkTypeLevelId, plan.getEventWorkTypeLevelId())
          .isNull(plan.getEventWorkTypeLevelId() == null,
              CompetitionScoreRuleEntity::getEventWorkTypeLevelId)
          .eq(CompetitionScoreRuleEntity::getStatus, "published")
          .eq(CompetitionScoreRuleEntity::getDeleted, 0));
      if (rules == null || rules.size() != 1) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "当前方案必须绑定唯一的已发布计分规则");
      }
      rule = rules.get(0);
      plan.setScoreRuleId(rule.getId());
    } else {
      rule = scoreRuleMapper.selectOne(new LambdaQueryWrapper<CompetitionScoreRuleEntity>()
          .eq(CompetitionScoreRuleEntity::getTenantId, tenantId)
          .eq(CompetitionScoreRuleEntity::getId, plan.getScoreRuleId())
          .eq(CompetitionScoreRuleEntity::getEventId, stage.getEventId())
          .eq(CompetitionScoreRuleEntity::getStageId, stage.getId())
          .eq(CompetitionScoreRuleEntity::getEventWorkTypeId, plan.getEventWorkTypeId())
          .eq(plan.getEventWorkTypeLevelId() != null,
              CompetitionScoreRuleEntity::getEventWorkTypeLevelId, plan.getEventWorkTypeLevelId())
          .isNull(plan.getEventWorkTypeLevelId() == null,
              CompetitionScoreRuleEntity::getEventWorkTypeLevelId)
          .eq(CompetitionScoreRuleEntity::getStatus, "published")
          .eq(CompetitionScoreRuleEntity::getDeleted, 0));
      if (rule == null) throw new BusinessException(ErrorCode.CONFLICT, "计分规则不属于当前执行方案范围");
    }
    List<CompetitionStageEntity> stages = stageMapper.selectList(new LambdaQueryWrapper<CompetitionStageEntity>()
        .eq(CompetitionStageEntity::getTenantId, tenantId)
        .eq(CompetitionStageEntity::getEventId, stage.getEventId())
        .eq(CompetitionStageEntity::getDeleted, 0));
    CompetitionStageEntity next = stages == null ? null : stages.stream()
        .filter(item -> item.getSortOrder() != null && stage.getSortOrder() != null
            && item.getSortOrder() > stage.getSortOrder())
        .min(java.util.Comparator.comparing(CompetitionStageEntity::getSortOrder)).orElse(null);
    if (next == null && plan.getPromotionQuota() != null && plan.getPromotionQuota() > 0) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "末赛段不能配置晋级人数");
    }
    plan.setTargetStageId(next == null ? null : next.getId());
    CompetitionPromotionRuleDomainService.ResolvedRule resolvedRule =
        new CompetitionPromotionRuleDomainService().resolve(
            plan.getPromotionRuleType(), plan.getPromotionQuota(), plan.getPromotionPercentage());
    if (resolvedRule.legacy()) {
      plan.setLegacyRule(1);
    } else {
      plan.setLegacyRule(0);
    }
    if (next == null && !resolvedRule.legacy()
        && !CompetitionPromotionRuleDomainService.FINAL_PODIUM.equals(resolvedRule.type())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "末赛段只能使用决赛前三名规则");
    }
    if (next != null && CompetitionPromotionRuleDomainService.FINAL_PODIUM.equals(resolvedRule.type())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "非末赛段不能使用决赛前三名规则");
    }
  }

  private List<CompetitionStageResourceBookingEntity> bookings(Long tenantId, Long planId) {
    List<CompetitionStageResourceBookingEntity> rows = bookingMapper.selectList(
        new LambdaQueryWrapper<CompetitionStageResourceBookingEntity>()
            .eq(CompetitionStageResourceBookingEntity::getTenantId, tenantId)
            .eq(CompetitionStageResourceBookingEntity::getPlanId, planId)
            .eq(CompetitionStageResourceBookingEntity::getDeleted, 0));
    return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
  }

  private void ensureBookingAvailable(Long tenantId, CompetitionStagePlanResourceEntity resource, Long planId) {
    Long conflict = bookingMapper.selectCount(new LambdaQueryWrapper<CompetitionStageResourceBookingEntity>()
        .eq(CompetitionStageResourceBookingEntity::getTenantId, tenantId)
        .eq(CompetitionStageResourceBookingEntity::getResourceType, resource.getResourceType())
        .eq(CompetitionStageResourceBookingEntity::getResourceId, resource.getResourceId())
        .eq(CompetitionStageResourceBookingEntity::getStatus, "reserved")
        .eq(CompetitionStageResourceBookingEntity::getDeleted, 0)
        .ne(CompetitionStageResourceBookingEntity::getPlanId, planId)
        .lt(CompetitionStageResourceBookingEntity::getStartAt, resource.getEndAt())
        .gt(CompetitionStageResourceBookingEntity::getEndAt, resource.getStartAt()));
    if (conflict != null && conflict > 0) throw new BusinessException(ErrorCode.CONFLICT, "资源时间发生冲突");
  }

  private void validateResource(CompetitionStageEntity stage, CompetitionStagePlanResourceItemReq item) {
    if (CompetitionStageContentType.fromCode(item.contentType()) == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "内容类型无效");
    if (item.resourceId() == null && (item.resourceCode() == null || item.resourceCode().isBlank())) throw new BusinessException(ErrorCode.BAD_REQUEST, "资源必须提供 ID 或编码");
    if ((item.startAt() == null) != (item.endAt() == null) || (item.startAt() != null && !item.endAt().isAfter(item.startAt()))) throw new BusinessException(ErrorCode.BAD_REQUEST, "资源时间窗口无效");
    if (item.startAt() != null && !withinStage(item.startAt(), item.endAt(), stage)) throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "资源时间必须落在赛段窗口内");
  }

  private boolean withinStage(LocalDateTime start, LocalDateTime end, CompetitionStageEntity stage) {
    return stage.getStartAt() == null || stage.getEndAt() == null
        || (!start.isBefore(stage.getStartAt()) && !end.isAfter(stage.getEndAt()));
  }

  private CompetitionStagePlanPreflightCheckVO check(String code, boolean passed, String message) {
    return new CompetitionStagePlanPreflightCheckVO(code, passed, message);
  }
}
