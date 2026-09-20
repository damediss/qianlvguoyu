package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.domain.CompetitionPracticalQuestionStatuses;
import com.guoyu.platform.competition.dto.CompetitionPracticalAssignmentReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalJudgeGroupReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalJudgeMemberReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalSessionCreateReq;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalAssessmentSheetEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreEntryEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreSheetEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeGroupEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionRegistrationEntity;
import com.guoyu.platform.competition.entity.CompetitionRosterItemEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionStageExecutionPlanEntity;
import com.guoyu.platform.competition.entity.CompetitionStagePaperAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionStageRosterItemEntity;
import com.guoyu.platform.competition.entity.CompetitionStageRosterSnapshotEntity;
import com.guoyu.platform.competition.enums.CompetitionStagePlanStatus;
import com.guoyu.platform.competition.enums.CompetitionStageStatus;
import com.guoyu.platform.competition.vo.CompetitionStageVO;
import com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalAssessmentSheetMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalScoreEntryMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalScoreSheetMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeGroupMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import com.guoyu.platform.competition.mapper.CompetitionRosterItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageExecutionPlanMapper;
import com.guoyu.platform.competition.mapper.CompetitionStagePaperAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageRosterItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageRosterSnapshotMapper;
import com.guoyu.platform.competition.vo.CompetitionPracticalConsoleQuestionVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalConsoleVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalConsoleTaskVO;
import com.guoyu.platform.security.SecurityContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import com.guoyu.platform.competition.realtime.CompetitionPracticalRealtimePublisher;

@Service
public class CompetitionPracticalConsoleService {
  private final CompetitionStageApplicationService stageService;
  private final CompetitionStageExecutionPlanMapper planMapper;
  private final CompetitionEventWorkTypeLevelMapper levelMapper;
  private final CompetitionStagePaperAssignmentMapper paperAssignmentMapper;
  private final CompetitionStageRosterItemMapper rosterItemMapper;
  private final CompetitionStageRosterSnapshotMapper rosterSnapshotMapper;
  private final CompetitionRefereeAssignmentMapper refereeAssignmentMapper;
  private final CompetitionRefereeGroupMapper refereeGroupMapper;
  private final CompetitionPracticalSessionMapper sessionMapper;
  private final CompetitionPracticalAssignmentMapper practicalAssignmentMapper;
  private final CompetitionPracticalScoreSheetMapper scoreSheetMapper;
  private final CompetitionPracticalExecutionService executionService;
  private final CompetitionContestantProfileMapper contestantProfileMapper;
  private final CompetitionRegistrationMapper registrationMapper;
  private final CompetitionRosterItemMapper rosterNameMapper;
  private final CompetitionEventMemberMapper eventMemberMapper;
  private final CompetitionRefereeProfileMapper refereeProfileMapper;
  private final CompetitionPracticalAssessmentSheetMapper assessmentSheetMapper;
  private final CompetitionPracticalScoreEntryMapper scoreEntryMapper;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private CompetitionPracticalRealtimePublisher realtimePublisher;

  @Autowired(required = false)
  public void setRealtimePublisher(CompetitionPracticalRealtimePublisher realtimePublisher) {
    this.realtimePublisher = realtimePublisher;
  }

  public CompetitionPracticalConsoleService(
      CompetitionStageApplicationService stageService,
      CompetitionStageExecutionPlanMapper planMapper,
      CompetitionEventWorkTypeLevelMapper levelMapper,
      CompetitionStagePaperAssignmentMapper paperAssignmentMapper,
      CompetitionStageRosterItemMapper rosterItemMapper,
      CompetitionStageRosterSnapshotMapper rosterSnapshotMapper,
      CompetitionRefereeAssignmentMapper refereeAssignmentMapper,
      CompetitionRefereeGroupMapper refereeGroupMapper,
      CompetitionPracticalSessionMapper sessionMapper,
      CompetitionPracticalAssignmentMapper practicalAssignmentMapper,
      CompetitionPracticalScoreSheetMapper scoreSheetMapper,
      CompetitionPracticalExecutionService executionService,
      CompetitionContestantProfileMapper contestantProfileMapper,
      CompetitionRegistrationMapper registrationMapper,
      CompetitionRosterItemMapper rosterNameMapper,
      CompetitionEventMemberMapper eventMemberMapper,
      CompetitionRefereeProfileMapper refereeProfileMapper,
      CompetitionPracticalAssessmentSheetMapper assessmentSheetMapper,
      CompetitionPracticalScoreEntryMapper scoreEntryMapper) {
    this.stageService = stageService;
    this.planMapper = planMapper;
    this.levelMapper = levelMapper;
    this.paperAssignmentMapper = paperAssignmentMapper;
    this.rosterItemMapper = rosterItemMapper;
    this.rosterSnapshotMapper = rosterSnapshotMapper;
    this.refereeAssignmentMapper = refereeAssignmentMapper;
    this.refereeGroupMapper = refereeGroupMapper;
    this.sessionMapper = sessionMapper;
    this.practicalAssignmentMapper = practicalAssignmentMapper;
    this.scoreSheetMapper = scoreSheetMapper;
    this.executionService = executionService;
    this.contestantProfileMapper = contestantProfileMapper;
    this.registrationMapper = registrationMapper;
    this.rosterNameMapper = rosterNameMapper;
    this.eventMemberMapper = eventMemberMapper;
    this.refereeProfileMapper = refereeProfileMapper;
    this.assessmentSheetMapper = assessmentSheetMapper;
    this.scoreEntryMapper = scoreEntryMapper;
  }

  @Transactional(readOnly = true)
  public CompetitionPracticalConsoleVO get(Long stageId, Long levelId) {
    var stage = stageService.get(stageId);
    CompetitionStageExecutionPlanEntity plan = publishedPlan(
        tenantId(), stage.getEventId(), stageId, stage.getEventWorkTypeId(), levelId);
    return plan == null ? empty(stage.getEventId(), stageId, stage.getEventWorkTypeId(), levelId)
        : summarize(plan);
  }

  @Transactional
  public CompetitionPracticalConsoleVO initialize(Long stageId, Long levelId) {
    var principal = SecurityContext.currentUser();
    var stage = ensureStageReadyForPracticalInitialization(stageService.assertCanManage(stageId));
    requireConfirmedStagePlan(principal.getTenantId(), stage.getEventId(),
        stage.getEventWorkTypeId(), levelId);

    List<CompetitionStagePaperAssignmentEntity> contestantPapers = paperAssignmentMapper
        .selectByScopeForUpdate(principal.getTenantId(), stage.getEventId(), stageId,
            stage.getEventWorkTypeId(), levelId, "practical");
    contestantPapers = safe(contestantPapers).stream()
        .filter(item -> "confirmed".equals(item.getStatus()))
        .toList();
    if (contestantPapers.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "请先确认当前赛段选手、实操试卷及考评表配置");
    }
    if (contestantPapers.stream().anyMatch(item -> item.getPaperSnapshotId() == null
        || item.getAssessmentSheetId() == null)) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "存在未绑定试卷或考评表的选手，不能初始化正式比赛");
    }

    List<CompetitionRefereeAssignmentEntity> refereeAssignments = activeRefereeAssignments(
        principal.getTenantId(), stage.getEventId(), stageId, stage.getEventWorkTypeId(), levelId);
    validateRefereeReadiness(contestantPapers, refereeAssignments);

    CompetitionStageExecutionPlanEntity plan = publishedPlan(
        principal.getTenantId(), stage.getEventId(), stageId,
        stage.getEventWorkTypeId(), levelId);
    CompetitionPracticalConsoleVO current = plan == null ? null : summarize(plan);
    if (current != null && current.scoringTaskCount() > 0) return current;
    plan = ensureRuntimePlan(principal.getTenantId(), principal.getUserId(), stage.getEventId(),
        stageId, stage.getEventWorkTypeId(), levelId, contestantPapers);
    executionService.freezePlanScoreRule(principal.getTenantId(), principal.getUserId(), plan);
    CompetitionStageExecutionPlanEntity runtimePlan = plan;

    List<CompetitionStageRosterItemEntity> rosterItems = safe(rosterItemMapper.selectList(
        new LambdaQueryWrapper<CompetitionStageRosterItemEntity>()
            .eq(CompetitionStageRosterItemEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionStageRosterItemEntity::getSnapshotId, plan.getRosterSnapshotId())
            .eq(CompetitionStageRosterItemEntity::getInclusionStatus, "included")
            .eq(CompetitionStageRosterItemEntity::getDeleted, 0)));
    Map<Long, CompetitionStageRosterItemEntity> rosterByContestant = rosterItems.stream()
        .collect(Collectors.toMap(CompetitionStageRosterItemEntity::getContestantMemberId,
            item -> item, (left, right) -> left, LinkedHashMap::new));
    Map<Long, CompetitionPracticalSessionEntity> sessions = new LinkedHashMap<>();
    int sessionCapacity = contestantPapers.size();
    int groupNo = 0;
    for (CompetitionStagePaperAssignmentEntity contestant : contestantPapers) {
      CompetitionStageRosterItemEntity roster = rosterByContestant.get(
          contestant.getContestantMemberId());
      if (roster == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "选手不在初始化生成的赛段名单快照中，不能初始化正式比赛");
      }
      Long configuredGroupId = refereeGroupFor(refereeAssignments, contestant.getContestantMemberId());
      if (configuredGroupId == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "选手必须先绑定已确定的裁判组，不能初始化正式比赛");
      }
      CompetitionRefereeGroupEntity configuredGroup = refereeGroupMapper.selectById(configuredGroupId);
      List<Long> refereeIds = refereesFor(refereeAssignments,
          contestant.getContestantMemberId());
      Long leaderMemberId = resolveLeaderMemberId(configuredGroup, refereeIds);
      if (configuredGroup == null
          || !Objects.equals(configuredGroup.getTenantId(), principal.getTenantId())
          || !Objects.equals(configuredGroup.getDeleted(), 0)
          || leaderMemberId == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "选手绑定的裁判组未指定组长，不能初始化正式比赛");
      }
      if (refereeIds.size() != 3) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "每名选手必须关联且只能关联3名正式裁判");
      }
      if (!refereeIds.contains(leaderMemberId)) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "裁判组组长不在当前选手的3名裁判中，不能初始化正式比赛");
      }
      CompetitionPracticalSessionEntity session = sessions.computeIfAbsent(
          contestant.getPaperSnapshotId(), paperId -> createSession(
              principal.getTenantId(), principal.getUserId(), runtimePlan, stage.getName(),
              stage.getStartAt(), stage.getEndAt(), paperId,
              contestant.getAssessmentSheetId(), sessionCapacity));
      var group = executionService.createGroup(principal.getTenantId(), principal.getUserId(),
          session.getId(), new CompetitionPracticalJudgeGroupReq(++groupNo,
              "选手" + contestant.getContestantMemberId() + "裁判组",
              refereeIds.stream().map(id -> new CompetitionPracticalJudgeMemberReq(id,
                      Objects.equals(id, leaderMemberId)
                          ? "leader" : "member"))
                  .toList()), plan.getId());
      List<CompetitionPracticalAssignmentEntity> created = executionService.assign(
          principal.getTenantId(), principal.getUserId(),
          new CompetitionPracticalAssignmentReq(session.getId(), group.getId(), roster.getId(),
              contestant.getContestantMemberId(), "P-" + groupNo, refereeIds), plan.getId());
      for (CompetitionPracticalAssignmentEntity task : created) {
        task.setAssessmentSheetId(contestant.getAssessmentSheetId());
        task.setUpdatedBy(principal.getUserId());
        if (practicalAssignmentMapper.updateById(task) != 1) {
          throw new BusinessException(ErrorCode.CONFLICT, "实操评分任务关联考评表失败");
        }
      }
    }
    CompetitionPracticalConsoleVO result = summarize(plan);
    if (realtimePublisher != null) {
      realtimePublisher.publish(principal.getTenantId(), "competition-initialized", null);
    }
    return result;
  }

  private CompetitionPracticalSessionEntity createSession(
      Long tenantId, Long userId, CompetitionStageExecutionPlanEntity plan, String stageName,
      LocalDateTime startAt, LocalDateTime endAt, Long paperId, Long assessmentSheetId,
      int capacity) {
    LocalDateTime start = startAt == null ? LocalDateTime.now() : startAt;
    LocalDateTime end = endAt == null || !endAt.isAfter(start) ? start.plusHours(8) : endAt;
    CompetitionPracticalSessionEntity session = executionService.createSession(
        tenantId, userId, plan.getEventId(), plan.getStageId(),
        new CompetitionPracticalSessionCreateReq(plan.getId(),
            "P-" + plan.getStageId() + "-" + paperId,
            (stageName == null ? "实操比赛" : stageName) + "-" + paperId,
            start, end, capacity, null), plan.getEventWorkTypeId(),
        plan.getEventWorkTypeLevelId());
    executionService.bindPaperSnapshot(tenantId, userId, session.getId(), paperId);
    session.setAssessmentSheetId(assessmentSheetId);
    session.setUpdatedBy(userId);
    if (sessionMapper.updateById(session) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操场次关联考评表失败");
    }
    return executionService.publishSession(tenantId, session.getId());
  }

  private List<Long> refereesFor(
      List<CompetitionRefereeAssignmentEntity> rows, Long contestantMemberId) {
    List<CompetitionRefereeAssignmentEntity> specific = rows.stream()
        .filter(item -> Objects.equals(contestantMemberId, item.getContestantMemberId()))
        .toList();
    List<CompetitionRefereeAssignmentEntity> selected = specific.isEmpty()
        ? rows.stream().filter(item -> item.getContestantMemberId() == null).toList()
        : specific;
    Set<Long> ids = new LinkedHashSet<>();
    selected.forEach(item -> ids.add(item.getRefereeMemberId()));
    return new ArrayList<>(ids);
  }

  private Long refereeGroupFor(
      List<CompetitionRefereeAssignmentEntity> rows, Long contestantMemberId) {
    List<CompetitionRefereeAssignmentEntity> specific = rows.stream()
        .filter(item -> Objects.equals(contestantMemberId, item.getContestantMemberId()))
        .toList();
    List<CompetitionRefereeAssignmentEntity> selected = specific.isEmpty()
        ? rows.stream().filter(item -> item.getContestantMemberId() == null).toList()
        : specific;
    Set<Long> groupIds = selected.stream()
        .map(CompetitionRefereeAssignmentEntity::getRefereeGroupId)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    return groupIds.size() == 1 && selected.size() == 3 ? groupIds.iterator().next() : null;
  }

  private Long resolveLeaderMemberId(CompetitionRefereeGroupEntity group, List<Long> refereeIds) {
    if (group == null || refereeIds == null || refereeIds.isEmpty()) return null;
    if (group.getLeaderRefereeMemberId() != null && refereeIds.contains(group.getLeaderRefereeMemberId())) {
      return group.getLeaderRefereeMemberId();
    }
    if (group.getLeaderRefereeProfileId() == null) return null;
    CompetitionRefereeProfileEntity profile = refereeProfileMapper.selectById(group.getLeaderRefereeProfileId());
    if (profile == null || profile.getPersonId() == null || !Objects.equals(profile.getDeleted(), 0)) {
      return null;
    }
    List<CompetitionEventMemberEntity> members = eventMemberMapper.selectList(
        new LambdaQueryWrapper<CompetitionEventMemberEntity>()
            .in(CompetitionEventMemberEntity::getId, refereeIds)
            .eq(CompetitionEventMemberEntity::getPersonId, profile.getPersonId())
            .eq(CompetitionEventMemberEntity::getRoleType, "referee")
            .eq(CompetitionEventMemberEntity::getDeleted, 0));
    if (members == null || members.isEmpty() || members.get(0) == null) return null;
    return members.get(0).getId();
  }

  private CompetitionStageExecutionPlanEntity publishedPlan(
      Long tenantId, Long eventId, Long stageId, Long workTypeId, Long levelId) {
    return planMapper.selectOne(new LambdaQueryWrapper<CompetitionStageExecutionPlanEntity>()
        .eq(CompetitionStageExecutionPlanEntity::getTenantId, tenantId)
        .eq(CompetitionStageExecutionPlanEntity::getEventId, eventId)
        .eq(CompetitionStageExecutionPlanEntity::getStageId, stageId)
        .eq(CompetitionStageExecutionPlanEntity::getEventWorkTypeId, workTypeId)
        .eq(CompetitionStageExecutionPlanEntity::getEventWorkTypeLevelId, levelId)
        .eq(CompetitionStageExecutionPlanEntity::getStatus, "published")
        .eq(CompetitionStageExecutionPlanEntity::getDeleted, 0)
        .orderByDesc(CompetitionStageExecutionPlanEntity::getPlanVersion)
        .last("LIMIT 1"));
  }

  private List<CompetitionRefereeAssignmentEntity> activeRefereeAssignments(
      Long tenantId, Long eventId, Long stageId, Long workTypeId, Long levelId) {
    return safe(refereeAssignmentMapper.selectList(
        new LambdaQueryWrapper<CompetitionRefereeAssignmentEntity>()
            .eq(CompetitionRefereeAssignmentEntity::getTenantId, tenantId)
            .eq(CompetitionRefereeAssignmentEntity::getEventId, eventId)
            .eq(CompetitionRefereeAssignmentEntity::getStageId, stageId)
            .eq(CompetitionRefereeAssignmentEntity::getEventWorkTypeId, workTypeId)
            .eq(CompetitionRefereeAssignmentEntity::getEventWorkTypeLevelId, levelId)
            .eq(CompetitionRefereeAssignmentEntity::getStatus, "active")
            .eq(CompetitionRefereeAssignmentEntity::getDeleted, 0)));
  }

  private CompetitionStageVO ensureStageReadyForPracticalInitialization(CompetitionStageVO stage) {
    CompetitionStageStatus status = CompetitionStageStatus.fromCode(stage == null ? null : stage.getStatus());
    if (status == CompetitionStageStatus.DRAFT) {
      return stageService.beginConfiguration(stage.getId(), "实操正式比赛初始化");
    }
    if (status == CompetitionStageStatus.CONFIGURING || status == CompetitionStageStatus.READY_LOCKED) {
      return stage;
    }
    throw new BusinessException(ErrorCode.CONFLICT, "当前赛段已开始或已结束，不能初始化正式比赛");
  }

  private void requireConfirmedStagePlan(
      Long tenantId, Long eventId, Long workTypeId, Long levelId) {
    CompetitionEventWorkTypeLevelEntity level = levelMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventWorkTypeLevelEntity>()
            .eq(CompetitionEventWorkTypeLevelEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeLevelEntity::getEventId, eventId)
            .eq(CompetitionEventWorkTypeLevelEntity::getEventWorkTypeId, workTypeId)
            .eq(CompetitionEventWorkTypeLevelEntity::getId, levelId)
            .eq(CompetitionEventWorkTypeLevelEntity::getDeleted, 0));
    if (level == null || !CompetitionStagePlanStatus.CONFIRMED.getCode()
        .equals(level.getStagePlanStatus())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "请先确认当前工种等级的赛段编排");
    }
  }

  private void validateRefereeReadiness(
      List<CompetitionStagePaperAssignmentEntity> contestantPapers,
      List<CompetitionRefereeAssignmentEntity> refereeAssignments) {
    for (CompetitionStagePaperAssignmentEntity contestant : contestantPapers) {
      if (refereeGroupFor(refereeAssignments, contestant.getContestantMemberId()) == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "选手必须先绑定已确定的裁判组，不能初始化正式比赛");
      }
      if (refereesFor(refereeAssignments, contestant.getContestantMemberId()).size() != 3) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "每名选手必须关联且只能关联3名正式裁判");
      }
    }
  }

  /**
   * Practical configuration is the source of truth for runtime initialization.
   * The generic execution-plan tables are still used by the runtime, but their
   * plan and roster snapshot are materialized here instead of requiring a
   * second manual publish workflow.
   */
  private CompetitionStageExecutionPlanEntity ensureRuntimePlan(
      Long tenantId, Long userId, Long eventId, Long stageId, Long workTypeId, Long levelId,
      List<CompetitionStagePaperAssignmentEntity> contestantPapers) {
    CompetitionStageExecutionPlanEntity plan = publishedPlan(
        tenantId, eventId, stageId, workTypeId, levelId);
    if (plan == null) {
      plan = planMapper.selectOne(new LambdaQueryWrapper<CompetitionStageExecutionPlanEntity>()
          .eq(CompetitionStageExecutionPlanEntity::getTenantId, tenantId)
          .eq(CompetitionStageExecutionPlanEntity::getEventId, eventId)
          .eq(CompetitionStageExecutionPlanEntity::getStageId, stageId)
          .eq(CompetitionStageExecutionPlanEntity::getEventWorkTypeId, workTypeId)
          .eq(CompetitionStageExecutionPlanEntity::getEventWorkTypeLevelId, levelId)
          .eq(CompetitionStageExecutionPlanEntity::getStatus, "draft")
          .eq(CompetitionStageExecutionPlanEntity::getDeleted, 0)
          .orderByDesc(CompetitionStageExecutionPlanEntity::getPlanVersion)
          .last("LIMIT 1"));
    }
    LocalDateTime now = LocalDateTime.now();
    if (plan == null) {
      plan = new CompetitionStageExecutionPlanEntity();
      plan.setId(IdWorker.getId());
      plan.setTenantId(tenantId);
      plan.setEventId(eventId);
      plan.setStageId(stageId);
      plan.setEventWorkTypeId(workTypeId);
      plan.setEventWorkTypeLevelId(levelId);
      plan.setPlanVersion(nextPlanVersion(tenantId, eventId, stageId, workTypeId, levelId));
      plan.setResourceVersion("practical-runtime");
      plan.setCreatedBy(userId);
      plan.setCreatedAt(now);
      plan.setDeleted(0);
      plan.setPreflightStatus("passed");
      plan.setStatus("published");
      plan.setPublishedAt(now);
      plan.setPublishedBy(userId);
      plan.setUpdatedBy(userId);
      plan.setUpdatedAt(now);
      if (planMapper.insert(plan) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "实操比赛执行方案创建失败，请刷新后重试");
      }
    } else if (!"published".equals(plan.getStatus())) {
      plan.setStatus("published");
      plan.setPreflightStatus("passed");
      plan.setPublishedAt(now);
      plan.setPublishedBy(userId);
      plan.setUpdatedBy(userId);
      plan.setUpdatedAt(now);
      if (planMapper.updateById(plan) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "实操比赛执行方案状态已发生变化，请刷新后重试");
      }
    }
    ensureRosterSnapshot(tenantId, userId, plan, contestantPapers, now);
    return plan;
  }

  private int nextPlanVersion(
      Long tenantId, Long eventId, Long stageId, Long workTypeId, Long levelId) {
    List<CompetitionStageExecutionPlanEntity> plans = safe(planMapper.selectList(
        new LambdaQueryWrapper<CompetitionStageExecutionPlanEntity>()
            .eq(CompetitionStageExecutionPlanEntity::getTenantId, tenantId)
            .eq(CompetitionStageExecutionPlanEntity::getEventId, eventId)
            .eq(CompetitionStageExecutionPlanEntity::getStageId, stageId)
            .eq(CompetitionStageExecutionPlanEntity::getEventWorkTypeId, workTypeId)
            .eq(CompetitionStageExecutionPlanEntity::getEventWorkTypeLevelId, levelId)
            .eq(CompetitionStageExecutionPlanEntity::getDeleted, 0)));
    return plans.stream().map(CompetitionStageExecutionPlanEntity::getPlanVersion)
        .filter(Objects::nonNull).max(Integer::compareTo).orElse(0) + 1;
  }

  private void ensureRosterSnapshot(
      Long tenantId, Long userId, CompetitionStageExecutionPlanEntity plan,
      List<CompetitionStagePaperAssignmentEntity> contestantPapers, LocalDateTime now) {
    Set<Long> contestantIds = contestantPapers.stream()
        .map(CompetitionStagePaperAssignmentEntity::getContestantMemberId)
        .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    CompetitionStageRosterSnapshotEntity snapshot = plan.getRosterSnapshotId() == null ? null
        : rosterSnapshotMapper.selectOne(new LambdaQueryWrapper<CompetitionStageRosterSnapshotEntity>()
            .eq(CompetitionStageRosterSnapshotEntity::getTenantId, tenantId)
            .eq(CompetitionStageRosterSnapshotEntity::getId, plan.getRosterSnapshotId())
            .eq(CompetitionStageRosterSnapshotEntity::getPlanId, plan.getId())
            .eq(CompetitionStageRosterSnapshotEntity::getStatus, "published")
            .eq(CompetitionStageRosterSnapshotEntity::getDeleted, 0));
    if (snapshot != null && rosterMatches(tenantId, snapshot.getId(), contestantIds)) return;

    List<CompetitionStageRosterSnapshotEntity> oldSnapshots = safe(
        rosterSnapshotMapper.selectList(new LambdaQueryWrapper<CompetitionStageRosterSnapshotEntity>()
            .eq(CompetitionStageRosterSnapshotEntity::getTenantId, tenantId)
            .eq(CompetitionStageRosterSnapshotEntity::getPlanId, plan.getId())
            .eq(CompetitionStageRosterSnapshotEntity::getStatus, "published")
            .eq(CompetitionStageRosterSnapshotEntity::getDeleted, 0)));
    for (CompetitionStageRosterSnapshotEntity old : oldSnapshots) {
      old.setStatus("superseded");
      old.setUpdatedBy(userId);
      old.setUpdatedAt(now);
      rosterSnapshotMapper.updateById(old);
    }

    snapshot = new CompetitionStageRosterSnapshotEntity();
    snapshot.setId(IdWorker.getId());
    snapshot.setTenantId(tenantId);
    snapshot.setPlanId(plan.getId());
    snapshot.setEventId(plan.getEventId());
    snapshot.setStageId(plan.getStageId());
    snapshot.setEventWorkTypeId(plan.getEventWorkTypeId());
    snapshot.setEventWorkTypeLevelId(plan.getEventWorkTypeLevelId());
    snapshot.setSnapshotVersion(nextRosterVersion(tenantId, plan.getId()));
    snapshot.setSourceType("manual");
    snapshot.setStatus("published");
    snapshot.setGeneratedAt(now);
    snapshot.setPublishedAt(now);
    snapshot.setPublishedBy(userId);
    snapshot.setCreatedBy(userId);
    snapshot.setUpdatedBy(userId);
    snapshot.setCreatedAt(now);
    snapshot.setUpdatedAt(now);
    snapshot.setDeleted(0);
    if (rosterSnapshotMapper.insert(snapshot) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操比赛选手名单准备失败，请刷新后重试");
    }
    for (CompetitionStagePaperAssignmentEntity paper : contestantPapers) {
      CompetitionStageRosterItemEntity item = new CompetitionStageRosterItemEntity();
      item.setId(IdWorker.getId());
      item.setTenantId(tenantId);
      item.setSnapshotId(snapshot.getId());
      item.setEventId(plan.getEventId());
      item.setStageId(plan.getStageId());
      item.setEventWorkTypeId(plan.getEventWorkTypeId());
      item.setEventWorkTypeLevelId(plan.getEventWorkTypeLevelId());
      item.setContestantMemberId(paper.getContestantMemberId());
      item.setContestantProfileId(paper.getContestantProfileId());
      item.setRegistrationId(paper.getRegistrationId());
      item.setInclusionStatus("included");
      item.setEligibilityStatus("eligible");
      item.setCreatedBy(userId);
      item.setUpdatedBy(userId);
      item.setCreatedAt(now);
      item.setUpdatedAt(now);
      item.setDeleted(0);
      if (rosterItemMapper.insert(item) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "实操比赛选手名单项创建失败，请刷新后重试");
      }
    }
    plan.setRosterSnapshotId(snapshot.getId());
    plan.setUpdatedBy(userId);
    plan.setUpdatedAt(now);
    if (planMapper.updateById(plan) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操比赛执行方案名单关联失败，请刷新后重试");
    }
  }

  private boolean rosterMatches(Long tenantId, Long snapshotId, Set<Long> expectedContestantIds) {
    List<CompetitionStageRosterItemEntity> items = safe(rosterItemMapper.selectList(
        new LambdaQueryWrapper<CompetitionStageRosterItemEntity>()
            .eq(CompetitionStageRosterItemEntity::getTenantId, tenantId)
            .eq(CompetitionStageRosterItemEntity::getSnapshotId, snapshotId)
            .eq(CompetitionStageRosterItemEntity::getInclusionStatus, "included")
            .eq(CompetitionStageRosterItemEntity::getDeleted, 0)));
    Set<Long> actualContestantIds = items.stream()
        .map(CompetitionStageRosterItemEntity::getContestantMemberId)
        .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    return actualContestantIds.equals(expectedContestantIds);
  }

  private int nextRosterVersion(Long tenantId, Long planId) {
    List<CompetitionStageRosterSnapshotEntity> snapshots = safe(
        rosterSnapshotMapper.selectList(new LambdaQueryWrapper<CompetitionStageRosterSnapshotEntity>()
            .eq(CompetitionStageRosterSnapshotEntity::getTenantId, tenantId)
            .eq(CompetitionStageRosterSnapshotEntity::getPlanId, planId)
            .eq(CompetitionStageRosterSnapshotEntity::getDeleted, 0)));
    return snapshots.stream().map(CompetitionStageRosterSnapshotEntity::getSnapshotVersion)
        .filter(Objects::nonNull).max(Integer::compareTo).orElse(0) + 1;
  }

  private CompetitionPracticalConsoleVO summarize(CompetitionStageExecutionPlanEntity plan) {
    List<CompetitionPracticalSessionEntity> sessions = safe(sessionMapper.selectList(
        new LambdaQueryWrapper<CompetitionPracticalSessionEntity>()
            .eq(CompetitionPracticalSessionEntity::getTenantId, plan.getTenantId())
            .eq(CompetitionPracticalSessionEntity::getPlanId, plan.getId())
            .eq(CompetitionPracticalSessionEntity::getDeleted, 0)));
    List<CompetitionPracticalAssignmentEntity> assignments = safe(
        practicalAssignmentMapper.selectList(
            new LambdaQueryWrapper<CompetitionPracticalAssignmentEntity>()
                .eq(CompetitionPracticalAssignmentEntity::getTenantId, plan.getTenantId())
                .eq(CompetitionPracticalAssignmentEntity::getPlanId, plan.getId())
                .eq(CompetitionPracticalAssignmentEntity::getDeleted, 0)));
    Set<Long> contestants = assignments.stream()
        .map(CompetitionPracticalAssignmentEntity::getContestantMemberId)
        .collect(Collectors.toSet());
    List<CompetitionPracticalScoreSheetEntity> sheets = assignments.isEmpty() ? List.of()
        : safe(scoreSheetMapper.selectList(
            new LambdaQueryWrapper<CompetitionPracticalScoreSheetEntity>()
                .eq(CompetitionPracticalScoreSheetEntity::getTenantId, plan.getTenantId())
                .eq(CompetitionPracticalScoreSheetEntity::getPlanId, plan.getId())
                .eq(CompetitionPracticalScoreSheetEntity::getDeleted, 0)));
    int submitted = (int) sheets.stream()
        .filter(item -> "submitted_locked".equals(item.getStatus()))
        .count();
    String status = assignments.isEmpty() ? "not_initialized"
        : submitted == assignments.size() ? "completed" : "initialized";
    Map<Long, CompetitionPracticalScoreSheetEntity> sheetByAssignment = sheets.stream()
        .collect(Collectors.toMap(CompetitionPracticalScoreSheetEntity::getAssignmentId,
            item -> item, (left, right) -> left));
    DisplayLookup lookup = loadDisplayLookup(plan, assignments, sheets);
    List<CompetitionPracticalConsoleTaskVO> tasks = assignments.stream().map(item -> {
      CompetitionPracticalScoreSheetEntity sheet = sheetByAssignment.get(item.getId());
      CompetitionPracticalAssessmentSheetEntity assessment =
          lookup.assessmentSheets.get(item.getAssessmentSheetId());
      List<CompetitionPracticalScoreEntryEntity> entries = sheet == null
          ? List.of()
          : lookup.entriesBySheet.getOrDefault(sheet.getId(), List.of());
      return new CompetitionPracticalConsoleTaskVO(id(item.getId()), id(item.getSessionId()),
          id(item.getContestantMemberId()), id(item.getRefereeMemberId()),
          id(item.getAssessmentSheetId()), item.getWorkstationCode(), item.getStatus(),
          sheet == null ? null : sheet.getStatus(), sheet == null ? null : sheet.getTotalScore(),
          lookup.contestantNos.get(item.getContestantMemberId()),
          lookup.contestantNames.get(item.getContestantMemberId()),
          lookup.refereeNames.get(item.getRefereeMemberId()),
          assessment == null ? null : assessment.getTitle(),
          questionsOf(assessment, sheet, entries));
    }).toList();
    return new CompetitionPracticalConsoleVO(id(plan.getId()), id(plan.getEventId()),
        id(plan.getStageId()), id(plan.getEventWorkTypeId()),
        id(plan.getEventWorkTypeLevelId()), sessions.size(), contestants.size(),
        assignments.size(), sheets.size(), submitted, status, tasks);
  }

  private DisplayLookup loadDisplayLookup(
      CompetitionStageExecutionPlanEntity plan,
      List<CompetitionPracticalAssignmentEntity> assignments,
      List<CompetitionPracticalScoreSheetEntity> sheets) {
    DisplayLookup lookup = new DisplayLookup();
    Set<Long> contestantIds = assignments.stream()
        .map(CompetitionPracticalAssignmentEntity::getContestantMemberId)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    Set<Long> refereeIds = assignments.stream()
        .map(CompetitionPracticalAssignmentEntity::getRefereeMemberId)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    Set<Long> assessmentIds = assignments.stream()
        .map(CompetitionPracticalAssignmentEntity::getAssessmentSheetId)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    Set<Long> scoreSheetIds = sheets.stream()
        .map(CompetitionPracticalScoreSheetEntity::getId)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    List<CompetitionContestantProfileEntity> profiles = contestantIds.isEmpty() ? List.of()
        : safe(contestantProfileMapper.selectList(
            new LambdaQueryWrapper<CompetitionContestantProfileEntity>()
                .eq(CompetitionContestantProfileEntity::getTenantId, plan.getTenantId())
                .in(CompetitionContestantProfileEntity::getMemberId, contestantIds)
                .eq(CompetitionContestantProfileEntity::getDeleted, 0)));
    Map<Long, CompetitionContestantProfileEntity> profileByMember = new LinkedHashMap<>();
    for (CompetitionContestantProfileEntity profile : profiles) {
      if (plan.getEventWorkTypeId() != null
          && profile.getEventWorkTypeId() != null
          && !Objects.equals(plan.getEventWorkTypeId(), profile.getEventWorkTypeId())) {
        continue;
      }
      profileByMember.putIfAbsent(profile.getMemberId(), profile);
      if (StringUtils.hasText(profile.getContestantNo())) {
        lookup.contestantNos.putIfAbsent(profile.getMemberId(), profile.getContestantNo());
      }
    }
    Set<Long> registrationIds = profileByMember.values().stream()
        .map(CompetitionContestantProfileEntity::getRegistrationId)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    List<CompetitionRegistrationEntity> registrations = registrationIds.isEmpty() ? List.of()
        : safe(registrationMapper.selectList(
            new LambdaQueryWrapper<CompetitionRegistrationEntity>()
                .eq(CompetitionRegistrationEntity::getTenantId, plan.getTenantId())
                .in(CompetitionRegistrationEntity::getId, registrationIds)
                .eq(CompetitionRegistrationEntity::getDeleted, 0)));
    Map<Long, Long> rosterIdByRegistration = registrations.stream()
        .filter(item -> item.getRosterItemId() != null)
        .collect(Collectors.toMap(CompetitionRegistrationEntity::getId,
            CompetitionRegistrationEntity::getRosterItemId, (left, right) -> left));
    Set<Long> rosterIds = new LinkedHashSet<>(rosterIdByRegistration.values());
    Map<Long, String> rosterNames = new HashMap<>();
    if (!rosterIds.isEmpty()) {
      for (CompetitionRosterItemEntity roster : safe(rosterNameMapper.selectList(
          new LambdaQueryWrapper<CompetitionRosterItemEntity>()
              .eq(CompetitionRosterItemEntity::getTenantId, plan.getTenantId())
              .in(CompetitionRosterItemEntity::getId, rosterIds)
              .eq(CompetitionRosterItemEntity::getDeleted, 0)))) {
        if (StringUtils.hasText(roster.getName())) {
          rosterNames.put(roster.getId(), roster.getName().trim());
        }
      }
    }
    for (Map.Entry<Long, CompetitionContestantProfileEntity> entry : profileByMember.entrySet()) {
      Long rosterId = rosterIdByRegistration.get(entry.getValue().getRegistrationId());
      String name = rosterId == null ? null : rosterNames.get(rosterId);
      if (StringUtils.hasText(name)) {
        lookup.contestantNames.put(entry.getKey(), name);
      }
    }

    List<CompetitionEventMemberEntity> refereeMembers = refereeIds.isEmpty() ? List.of()
        : safe(eventMemberMapper.selectList(
            new LambdaQueryWrapper<CompetitionEventMemberEntity>()
                .eq(CompetitionEventMemberEntity::getTenantId, plan.getTenantId())
                .in(CompetitionEventMemberEntity::getId, refereeIds)
                .eq(CompetitionEventMemberEntity::getDeleted, 0)));
    Map<Long, Long> personByMember = refereeMembers.stream()
        .filter(item -> item.getPersonId() != null)
        .collect(Collectors.toMap(CompetitionEventMemberEntity::getId,
            CompetitionEventMemberEntity::getPersonId, (left, right) -> left));
    Set<Long> personIds = new LinkedHashSet<>(personByMember.values());
    Map<Long, String> refereeNameByPerson = new HashMap<>();
    if (!personIds.isEmpty()) {
      for (CompetitionRefereeProfileEntity profile : safe(refereeProfileMapper.selectList(
          new LambdaQueryWrapper<CompetitionRefereeProfileEntity>()
              .eq(CompetitionRefereeProfileEntity::getTenantId, plan.getTenantId())
              .in(CompetitionRefereeProfileEntity::getPersonId, personIds)
              .eq(CompetitionRefereeProfileEntity::getDeleted, 0)))) {
        if (StringUtils.hasText(profile.getRefereeName())) {
          refereeNameByPerson.putIfAbsent(profile.getPersonId(), profile.getRefereeName().trim());
        }
      }
    }
    personByMember.forEach((memberId, personId) -> {
      String name = refereeNameByPerson.get(personId);
      if (StringUtils.hasText(name)) {
        lookup.refereeNames.put(memberId, name);
      }
    });

    if (!assessmentIds.isEmpty()) {
      for (CompetitionPracticalAssessmentSheetEntity sheet : safe(assessmentSheetMapper.selectList(
          new LambdaQueryWrapper<CompetitionPracticalAssessmentSheetEntity>()
              .eq(CompetitionPracticalAssessmentSheetEntity::getTenantId, plan.getTenantId())
              .in(CompetitionPracticalAssessmentSheetEntity::getId, assessmentIds)
              .eq(CompetitionPracticalAssessmentSheetEntity::getDeleted, 0)))) {
        lookup.assessmentSheets.put(sheet.getId(), sheet);
      }
    }
    if (!scoreSheetIds.isEmpty()) {
      for (CompetitionPracticalScoreEntryEntity entry : safe(scoreEntryMapper.selectList(
          new LambdaQueryWrapper<CompetitionPracticalScoreEntryEntity>()
              .eq(CompetitionPracticalScoreEntryEntity::getTenantId, plan.getTenantId())
              .in(CompetitionPracticalScoreEntryEntity::getScoreSheetId, scoreSheetIds)
              .eq(CompetitionPracticalScoreEntryEntity::getDeleted, 0)))) {
        lookup.entriesBySheet
            .computeIfAbsent(entry.getScoreSheetId(), ignored -> new ArrayList<>())
            .add(entry);
      }
    }
    return lookup;
  }

  private List<CompetitionPracticalConsoleQuestionVO> questionsOf(
      CompetitionPracticalAssessmentSheetEntity assessment,
      CompetitionPracticalScoreSheetEntity sheet,
      List<CompetitionPracticalScoreEntryEntity> entries) {
    if (assessment == null || !StringUtils.hasText(assessment.getSheetJson())) {
      return List.of();
    }
    JsonNode root;
    try {
      root = objectMapper.readTree(assessment.getSheetJson());
    } catch (Exception ex) {
      return List.of();
    }
    JsonNode summary = root.path("summary");
    if (!summary.isArray() || summary.isEmpty()) {
      summary = root.path("questions");
    }
    if (!summary.isArray()) {
      return List.of();
    }
    Map<String, BigDecimal> scoreByQuestion = new HashMap<>();
    Map<String, List<CompetitionPracticalScoreEntryEntity>> entriesByQuestion = new HashMap<>();
    for (CompetitionPracticalScoreEntryEntity entry : entries) {
      if (entry.getPaperQuestionId() == null) {
        continue;
      }
      String questionId = String.valueOf(entry.getPaperQuestionId());
      entriesByQuestion.computeIfAbsent(questionId, ignored -> new ArrayList<>()).add(entry);
      if (entry.getScore() != null) {
        scoreByQuestion.merge(questionId, entry.getScore(), BigDecimal::add);
      }
    }
    String sheetStatus = sheet == null ? null : sheet.getStatus();
    List<CompetitionPracticalConsoleQuestionVO> questions = new ArrayList<>();
    int fallbackOrder = 0;
    for (JsonNode node : summary) {
      fallbackOrder++;
      String questionId = textValue(node, "paperQuestionId");
      int sortOrder = node.path("sortOrder").isNumber()
          ? node.path("sortOrder").asInt() : fallbackOrder;
      String title = plainText(textValue(node, "title"));
      if (!StringUtils.hasText(title)) {
        title = "试题" + sortOrder;
      }
      List<CompetitionPracticalScoreEntryEntity> questionEntries = questionId == null
          ? List.of() : entriesByQuestion.getOrDefault(questionId, List.of());
      questions.add(new CompetitionPracticalConsoleQuestionVO(
          questionId,
          sortOrder,
          title,
          decimalValue(node.path("score")),
          questionId == null ? null : scoreByQuestion.get(questionId),
          CompetitionPracticalQuestionStatuses.ofQuestion(sheetStatus, questionEntries)));
    }
    return questions;
  }

  private static String textValue(JsonNode node, String field) {
    JsonNode value = node.path(field);
    if (value.isMissingNode() || value.isNull()) {
      return null;
    }
    String text = value.asText();
    return StringUtils.hasText(text) ? text : null;
  }

  private static BigDecimal decimalValue(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    if (node.isNumber()) {
      return node.decimalValue();
    }
    try {
      return StringUtils.hasText(node.asText()) ? new BigDecimal(node.asText()) : null;
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static String plainText(String value) {
    if (!StringUtils.hasText(value)) {
      return value;
    }
    return value.replaceAll("<[^>]*>", " ")
        .replace("&nbsp;", " ")
        .replaceAll("\\s+", " ")
        .trim();
  }

  private static final class DisplayLookup {
    private final Map<Long, String> contestantNos = new HashMap<>();
    private final Map<Long, String> contestantNames = new HashMap<>();
    private final Map<Long, String> refereeNames = new HashMap<>();
    private final Map<Long, CompetitionPracticalAssessmentSheetEntity> assessmentSheets = new HashMap<>();
    private final Map<Long, List<CompetitionPracticalScoreEntryEntity>> entriesBySheet = new HashMap<>();
  }

  private CompetitionPracticalConsoleVO empty(
      Long eventId, Long stageId, Long workTypeId, Long levelId) {
    return new CompetitionPracticalConsoleVO(null, id(eventId), id(stageId), id(workTypeId),
        id(levelId), 0, 0, 0, 0, 0, "not_initialized", List.of());
  }

  private Long tenantId() { return SecurityContext.currentUser().getTenantId(); }
  private static String id(Long value) { return value == null ? null : String.valueOf(value); }
  private static <T> List<T> safe(List<T> values) {
    return values == null ? new ArrayList<>() : new ArrayList<>(values);
  }
}
