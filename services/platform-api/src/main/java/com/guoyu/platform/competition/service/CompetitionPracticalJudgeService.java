package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.domain.CompetitionPracticalQuestionStatuses;
import com.guoyu.platform.competition.dto.CompetitionPracticalScoreSaveReq;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalAssessmentSheetEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalJudgeGroupEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreEntryEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreSheetEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeGroupEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeGroupMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionRegistrationEntity;
import com.guoyu.platform.competition.entity.CompetitionRosterItemEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalAssessmentSheetMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalJudgeGroupMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalScoreSheetMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeGroupMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeGroupMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import com.guoyu.platform.competition.mapper.CompetitionRosterItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import com.guoyu.platform.competition.vo.CompetitionPracticalAssessmentSheetVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalJudgeScoreEntryVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalJudgeTaskVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalQuestionProgressVO;
import com.guoyu.platform.security.SecurityContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import com.guoyu.platform.competition.realtime.CompetitionPracticalRealtimePublisher;

@Service
public class CompetitionPracticalJudgeService {
  private static final Logger log = LoggerFactory.getLogger(CompetitionPracticalJudgeService.class);
  private static final int REQUIRED_JUDGES = 3;
  private final CompetitionRefereeProfileMapper profileMapper;
  private final CompetitionEventMemberMapper eventMemberMapper;
  private final CompetitionPracticalAssignmentMapper assignmentMapper;
  private final CompetitionPracticalJudgeGroupMapper groupMapper;
  private final CompetitionRefereeGroupMapper configuredGroupMapper;
  private final CompetitionRefereeGroupMemberMapper configuredGroupMemberMapper;
  private final CompetitionPracticalAssessmentSheetMapper assessmentSheetMapper;
  private final CompetitionPracticalScoreSheetMapper scoreSheetMapper;
  private final CompetitionPracticalSessionMapper sessionMapper;
  private final CompetitionStageMapper stageMapper;
  private final CompetitionEventMapper eventMapper;
  private final CompetitionEventWorkTypeMapper eventWorkTypeMapper;
  private final CompetitionEventWorkTypeLevelMapper eventWorkTypeLevelMapper;
  private final CompetitionContestantProfileMapper contestantProfileMapper;
  private final CompetitionRegistrationMapper registrationMapper;
  private final CompetitionRosterItemMapper rosterItemMapper;
  private final CompetitionPracticalAssessmentSheetService assessmentSheetService;
  private final CompetitionPracticalExecutionService executionService;
  private CompetitionPracticalStagePlanSnapshotService planSnapshotService;

  @Autowired(required = false)
  public void setPlanSnapshotService(
      CompetitionPracticalStagePlanSnapshotService planSnapshotService) {
    this.planSnapshotService = planSnapshotService;
  }

  private CompetitionPracticalRealtimePublisher realtimePublisher;

  @Autowired(required = false)
  public void setRealtimePublisher(CompetitionPracticalRealtimePublisher realtimePublisher) {
    this.realtimePublisher = realtimePublisher;
  }

  public CompetitionPracticalJudgeService(
      CompetitionRefereeProfileMapper profileMapper,
      CompetitionEventMemberMapper eventMemberMapper,
      CompetitionPracticalAssignmentMapper assignmentMapper,
      CompetitionPracticalJudgeGroupMapper groupMapper,
      CompetitionRefereeGroupMapper configuredGroupMapper,
      CompetitionRefereeGroupMemberMapper configuredGroupMemberMapper,
      CompetitionPracticalAssessmentSheetMapper assessmentSheetMapper,
      CompetitionPracticalScoreSheetMapper scoreSheetMapper,
      CompetitionPracticalSessionMapper sessionMapper,
      CompetitionStageMapper stageMapper,
      CompetitionEventMapper eventMapper,
      CompetitionEventWorkTypeMapper eventWorkTypeMapper,
      CompetitionEventWorkTypeLevelMapper eventWorkTypeLevelMapper,
      CompetitionContestantProfileMapper contestantProfileMapper,
      CompetitionRegistrationMapper registrationMapper,
      CompetitionRosterItemMapper rosterItemMapper,
      CompetitionPracticalAssessmentSheetService assessmentSheetService,
      CompetitionPracticalExecutionService executionService) {
    this.profileMapper = profileMapper;
    this.eventMemberMapper = eventMemberMapper;
    this.assignmentMapper = assignmentMapper;
    this.groupMapper = groupMapper;
    this.configuredGroupMapper = configuredGroupMapper;
    this.configuredGroupMemberMapper = configuredGroupMemberMapper;
    this.assessmentSheetMapper = assessmentSheetMapper;
    this.scoreSheetMapper = scoreSheetMapper;
    this.sessionMapper = sessionMapper;
    this.stageMapper = stageMapper;
    this.eventMapper = eventMapper;
    this.eventWorkTypeMapper = eventWorkTypeMapper;
    this.eventWorkTypeLevelMapper = eventWorkTypeLevelMapper;
    this.contestantProfileMapper = contestantProfileMapper;
    this.registrationMapper = registrationMapper;
    this.rosterItemMapper = rosterItemMapper;
    this.assessmentSheetService = assessmentSheetService;
    this.executionService = executionService;
  }

  @Transactional(readOnly = true)
  public List<CompetitionPracticalJudgeTaskVO> tasks() {
    CompetitionRefereeProfileEntity profile = currentProfile();
    List<CompetitionPracticalAssignmentEntity> assignments = currentAssignments(profile);
    if (assignments.isEmpty()) return List.of();
    JudgeTaskDisplayLookup lookup = loadDisplayLookup(tenantId(), assignments);
    CompetitionRefereeGroupEntity group = currentConfiguredGroup(profile);
    boolean leader = isConfiguredGroupLeader(group, profile);
    Long activeStageId = resolveActiveStageId(group, assignments, lookup);
    return toTaskVos(assignments, lookup, leader, activeStageId);
  }

  @Transactional
  public List<CompetitionPracticalJudgeTaskVO> selectActiveStage(String stageIdText) {
    Long stageId = parseStageId(stageIdText);
    CompetitionRefereeProfileEntity profile = currentProfile();
    CompetitionRefereeGroupEntity group = lockCurrentConfiguredGroup(profile);
    if (group == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "当前账号未加入裁判小组");
    }
    if (!isConfiguredGroupLeader(group, profile)) {
      log.warn("Competition practical stage switch rejected for non-leader, userId={}, groupId={}",
          userId(), group.getId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "仅裁判组组长可以切换赛段任务");
    }
    if ("closed".equals(group.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "裁判小组已解散，不能切换赛段任务");
    }
    List<CompetitionPracticalAssignmentEntity> assignments = currentAssignments(profile);
    JudgeTaskDisplayLookup lookup = loadDisplayLookup(tenantId(), assignments);
    Set<Long> available = availableStageIds(assignments, lookup);
    if (!available.contains(stageId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前小组未分配该赛段任务");
    }
    if (!Objects.equals(group.getActiveStageId(), stageId)) {
      group.setActiveStageId(stageId);
      group.setUpdatedBy(userId());
      if (configuredGroupMapper.updateById(group) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "赛段任务切换失败，请刷新后重试");
      }
    }
    publish("judge-active-stage-changed", null);
    return toTaskVos(assignments, lookup, true, stageId);
  }

  @Transactional
  public CompetitionPracticalJudgeTaskVO open(Long assignmentId) {
    CompetitionPracticalAssignmentEntity assignment = ownedAssignment(assignmentId);
    requireStarted(assignment);
    executionService.openSheet(tenantId(), userId(), assignmentId);
    return toVO(assignment, loadDisplayLookup(tenantId(), List.of(assignment)));
  }

  @Transactional
  public CompetitionPracticalJudgeTaskVO save(
      Long assignmentId, CompetitionPracticalScoreSaveReq req, boolean submit) {
    CompetitionPracticalAssignmentEntity assignment = ownedAssignment(assignmentId);
    requireStarted(assignment);
    CompetitionPracticalScoreSheetEntity sheet = scoreSheet(assignmentId);
    if (sheet == null) sheet = executionService.openSheet(tenantId(), userId(), assignmentId);
    if (submit) {
      sheet = executionService.submitQuestions(tenantId(), userId(), sheet.getId(), req);
      if ("submitted_locked".equals(sheet.getStatus())) {
        assignment.setStatus("completed");
        assignment.setEndedAt(LocalDateTime.now());
        assignment.setUpdatedBy(userId());
        if (assignmentMapper.updateById(assignment) != 1) {
          throw new BusinessException(ErrorCode.CONFLICT, "评分任务状态已变化，请刷新后重试");
        }
      }
    } else {
      executionService.saveSheet(tenantId(), userId(), sheet.getId(), req);
    }
    if (submit) publish("score-sheet-submitted", assignmentId);
    return toVO(assignment);
  }

  @Transactional
  public CompetitionPracticalJudgeTaskVO confirmStart(Long assignmentId) {
    CompetitionPracticalAssignmentEntity owned = ownedAssignment(assignmentId);
    List<CompetitionPracticalAssignmentEntity> assignments = lockCandidateAssignments(owned);
    CompetitionPracticalAssignmentEntity assignment = assignments.stream()
        .filter(item -> Objects.equals(item.getId(), owned.getId()))
        .findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "评分任务状态已变化，请刷新后重试"));
    String startStatus = startStatus(assignments);
    if (Set.of("started", "completed").contains(startStatus)) {
      return toVO(assignment, loadDisplayLookup(tenantId(), List.of(assignment)));
    }
    if (assignment.getStartConfirmedAt() == null) {
      assignment.setStartConfirmedAt(LocalDateTime.now());
      assignment.setStartConfirmedBy(userId());
      assignment.setUpdatedBy(userId());
      if (assignmentMapper.updateById(assignment) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "开考确认状态已变化，请刷新后重试");
      }
    }
    publish("judge-start-confirmed", assignmentId);
    return toVO(assignment, loadDisplayLookup(tenantId(), List.of(assignment)));
  }

  @Transactional
  public CompetitionPracticalJudgeTaskVO startSession(Long assignmentId) {
    CompetitionPracticalAssignmentEntity owned = ownedAssignment(assignmentId);
    CompetitionPracticalJudgeGroupEntity group = lockGroup(owned.getJudgeGroupId());
    if (!Objects.equals(group.getLeaderRefereeMemberId(), owned.getRefereeMemberId())) {
      log.warn("Competition practical start rejected for non-leader, assignmentId={}, refereeMemberId={}, groupId={}",
          assignmentId, owned.getRefereeMemberId(), owned.getJudgeGroupId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "仅裁判组组长可以开始考试");
    }
    List<CompetitionPracticalAssignmentEntity> assignments = lockCandidateAssignments(owned);
    if (assignments.size() != REQUIRED_JUDGES) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前选手必须分配3名有效裁判后才能开始考试");
    }
    String currentStatus = startStatus(assignments);
    if ("completed".equals(currentStatus)) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前选手的实操考试已完成");
    }
    if ("started".equals(currentStatus)) {
      return toVO(owned, loadDisplayLookup(tenantId(), List.of(owned)));
    }
    long confirmed = assignments.stream()
        .filter(item -> item.getStartConfirmedAt() != null)
        .count();
    if (confirmed != REQUIRED_JUDGES) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "三名裁判必须分别在各自设备确认后，组长才能开始考试");
    }
    executionService.ensureInteractiveStage(tenantId(), userId(), owned.getSessionId());
    LocalDateTime now = LocalDateTime.now();
    if (planSnapshotService == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛段实操运行快照服务不可用");
    }
    CompetitionPracticalStagePlanSnapshotService.RuntimeTimingSnapshot runtimeConfig =
        planSnapshotService.runtimeTimingForPlan(tenantId(), owned.getPlanId());
    if (runtimeConfig.durationSeconds() == null || runtimeConfig.durationSeconds() <= 0) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛段实操标准时长无效，无法启动考试");
    }
    LocalDateTime standardDeadlineAt = now.plusSeconds(runtimeConfig.durationSeconds());
    LocalDateTime deadlineAt = null;
    if ("AUTO_FINISH".equals(runtimeConfig.overtimeAction())) {
      if (runtimeConfig.maxDurationSeconds() == null
          || runtimeConfig.maxDurationSeconds() < runtimeConfig.durationSeconds()) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛段实操自动结束时限无效，无法启动考试");
      }
      deadlineAt = now.plusSeconds(runtimeConfig.maxDurationSeconds());
    }
    Long currentUserId = userId();
    for (CompetitionPracticalAssignmentEntity assignment : assignments) {
      assignment.setStatus("in_progress");
      assignment.setStartedAt(now);
      assignment.setStandardDeadlineAt(standardDeadlineAt);
      assignment.setDeadlineAt(deadlineAt);
      assignment.setExamEndedAt(null);
      assignment.setPausedDurationSeconds(0L);
      assignment.setStartedBy(currentUserId);
      assignment.setUpdatedBy(currentUserId);
      if (assignmentMapper.updateById(assignment) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "统一开考状态已变化，请刷新后重试");
      }
    }
    publish("practical-session-started", assignmentId);
    return toVO(owned, loadDisplayLookup(tenantId(), List.of(owned)));
  }

  @Transactional
  public CompetitionPracticalJudgeTaskVO completeSession(Long assignmentId) {
    CompetitionPracticalAssignmentEntity owned = ownedAssignment(assignmentId);
    CompetitionPracticalJudgeGroupEntity group = lockGroup(owned.getJudgeGroupId());
    if (!Objects.equals(group.getLeaderRefereeMemberId(), owned.getRefereeMemberId())) {
      log.warn("Competition practical completion rejected for non-leader, assignmentId={}, refereeMemberId={}, groupId={}",
          assignmentId, owned.getRefereeMemberId(), owned.getJudgeGroupId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "仅裁判组组长可以完成考试并提交实操成绩");
    }
    List<CompetitionPracticalAssignmentEntity> assignments = lockCandidateAssignments(owned);
    if (assignments.size() != REQUIRED_JUDGES
        || assignments.stream().anyMatch(item -> !"completed".equals(item.getStatus()))) {
      throw new BusinessException(ErrorCode.CONFLICT, "三名裁判全部提交后才能由组长完成考试");
    }
    executionService.completePracticalScore(tenantId(), userId(), owned.getId());
    publish("practical-session-completed", assignmentId);
    return toVO(owned, loadDisplayLookup(tenantId(), List.of(owned)));
  }

  private CompetitionRefereeProfileEntity currentProfile() {
    var principal = SecurityContext.currentUser();
    CompetitionRefereeProfileEntity profile = profileMapper.selectOne(
        new LambdaQueryWrapper<CompetitionRefereeProfileEntity>()
            .eq(CompetitionRefereeProfileEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionRefereeProfileEntity::getUserId, principal.getUserId())
            .eq(CompetitionRefereeProfileEntity::getStatus, "active")
            .eq(CompetitionRefereeProfileEntity::getDeleted, 0));
    if (profile == null) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号未关联有效竞赛裁判档案");
    }
    return profile;
  }

  private CompetitionPracticalAssignmentEntity ownedAssignment(Long assignmentId) {
    CompetitionPracticalAssignmentEntity assignment = assignmentMapper.selectById(assignmentId);
    List<Long> currentMemberIds = currentRefereeMemberIds();
    if (assignment == null || !Objects.equals(assignment.getTenantId(), tenantId())
        || !Objects.equals(assignment.getDeleted(), 0)
        || !currentMemberIds.contains(assignment.getRefereeMemberId())) {
      log.warn("Competition practical assignment ownership rejected, assignmentId={}, userId={}",
          assignmentId, userId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "评分任务不属于当前裁判");
    }
    return assignment;
  }

  private CompetitionPracticalJudgeTaskVO toVO(CompetitionPracticalAssignmentEntity assignment) {
    return toVO(assignment, loadDisplayLookup(tenantId(), List.of(assignment)));
  }

  private CompetitionPracticalJudgeTaskVO toVO(
      CompetitionPracticalAssignmentEntity assignment,
      JudgeTaskDisplayLookup lookup,
      Long activeStageId) {
    return toVO(assignment, lookup, activeStageId, loadScoringLookup(List.of(assignment), lookup));
  }

  private CompetitionPracticalJudgeTaskVO toVO(
      CompetitionPracticalAssignmentEntity assignment,
      JudgeTaskDisplayLookup lookup,
      Long activeStageId,
      ScoringLookup scoring) {
    CompetitionPracticalScoreSheetEntity score = scoring.sheetOf(assignment.getId());
    CompetitionPracticalJudgeGroupEntity group = scoring.groupOf(assignment.getJudgeGroupId());
    List<CompetitionPracticalAssignmentEntity> groupAssignments = scoring.candidatesOf(assignment);
    int confirmedJudges = (int) groupAssignments.stream()
        .filter(item -> item.getStartConfirmedAt() != null)
        .count();
    CompetitionPracticalAssessmentSheetVO assessmentVo = scoring.assessmentOf(assignment.getAssessmentSheetId());
    List<CompetitionPracticalScoreEntryEntity> entries = score == null
        ? List.of() : scoring.entriesOf(score.getId());
    List<BigDecimal> submittedScores = groupAssignments.stream()
        .map(item -> scoring.sheetOf(item.getId()))
        .filter(Objects::nonNull)
        .filter(item -> "submitted_locked".equals(item.getStatus()))
        .map(CompetitionPracticalScoreSheetEntity::getTotalScore)
        .filter(Objects::nonNull)
        .toList();
    CompetitionPracticalExecutionService.PracticalScoreDifferencePreview scoreDifference =
        scoring.previewOf(assignment, submittedScores);
    String groupStatus = startStatus(groupAssignments);
    if ("completed".equals(groupStatus)
        && !scoring.practicalCompleted(assignment.getContestantMemberId())) {
      groupStatus = "awaiting_leader";
    }
    JudgeTaskDisplay display = lookup.displayOf(assignment);
    return new CompetitionPracticalJudgeTaskVO(id(assignment.getId()), id(assignment.getSessionId()),
        id(display.eventId()), display.eventName(), id(display.stageId()), display.stageName(),
        display.workTypeName(), display.levelName(),
        id(assignment.getContestantMemberId()), display.contestantNo(), display.contestantName(),
        id(assignment.getRefereeMemberId()),
        assignment.getWorkstationCode(), assignment.getStatus(),
        id(assignment.getAssessmentSheetId()), assessmentVo, id(score == null ? null : score.getId()),
        score == null ? null : score.getStatus(), score == null ? null : score.getTotalScore(),
        scoreDifference.allowedDifference(), scoreDifference.actualDifference(), scoreDifference.status(),
        Objects.equals(group.getLeaderRefereeMemberId(), assignment.getRefereeMemberId())
            ? "leader" : "member",
        groupStatus, confirmedJudges, REQUIRED_JUDGES,
        assignment.getStartConfirmedAt() != null,
        questionProgress(assessmentVo, score, entries),
        id(activeStageId), assignment.getStartedAt(), assignment.getStandardDeadlineAt(),
        assignment.getDeadlineAt(), assignment.getExamEndedAt(),
        assignment.getPausedDurationSeconds() == null ? 0L : assignment.getPausedDurationSeconds());
  }

  private CompetitionPracticalJudgeTaskVO toVO(
      CompetitionPracticalAssignmentEntity assignment, JudgeTaskDisplayLookup lookup) {
    CompetitionRefereeProfileEntity profile = currentProfile();
    CompetitionRefereeGroupEntity configured = currentConfiguredGroup(profile);
    Long activeStageId = resolveActiveStageId(configured, List.of(assignment), lookup);
    return toVO(assignment, lookup, activeStageId);
  }

  private List<CompetitionPracticalJudgeTaskVO> toTaskVos(
      List<CompetitionPracticalAssignmentEntity> assignments,
      JudgeTaskDisplayLookup lookup,
      boolean leader,
      Long activeStageId) {
    List<CompetitionPracticalAssignmentEntity> visible = assignments;
    if (!leader && activeStageId != null) {
      visible = assignments.stream()
          .filter(item -> Objects.equals(lookup.displayOf(item).stageId(), activeStageId))
          .toList();
    }
    ScoringLookup scoring = loadScoringLookup(visible, lookup);
    return visible.stream().map(item -> toVO(item, lookup, activeStageId, scoring)).toList();
  }

  private List<CompetitionPracticalQuestionProgressVO> questionProgress(
      CompetitionPracticalAssessmentSheetVO assessment,
      CompetitionPracticalScoreSheetEntity sheet,
      List<CompetitionPracticalScoreEntryEntity> entries) {
    JsonNode questions = assessment == null || assessment.sheet() == null
        ? null : assessment.sheet().path("questions");
    if (questions == null || !questions.isArray() || questions.isEmpty()) {
      questions = assessment == null || assessment.sheet() == null
          ? null : assessment.sheet().path("summary");
    }
    if (questions == null || !questions.isArray()) {
      return List.of();
    }
    Map<String, List<CompetitionPracticalScoreEntryEntity>> entriesByQuestion = new LinkedHashMap<>();
    for (CompetitionPracticalScoreEntryEntity entry : entries) {
      if (entry.getPaperQuestionId() == null) {
        continue;
      }
      entriesByQuestion
          .computeIfAbsent(String.valueOf(entry.getPaperQuestionId()), ignored -> new ArrayList<>())
          .add(entry);
    }
    String sheetStatus = sheet == null ? null : sheet.getStatus();
    List<CompetitionPracticalQuestionProgressVO> progress = new ArrayList<>();
    for (JsonNode node : questions) {
      String questionId = node.path("paperQuestionId").asText("");
      if (questionId.isBlank()) {
        continue;
      }
      List<CompetitionPracticalScoreEntryEntity> questionEntries =
          entriesByQuestion.getOrDefault(questionId, List.of());
      BigDecimal score = questionEntries.stream()
          .map(CompetitionPracticalScoreEntryEntity::getScore)
          .filter(Objects::nonNull)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      List<CompetitionPracticalJudgeScoreEntryVO> entryVos = questionEntries.stream()
          .map(entry -> new CompetitionPracticalJudgeScoreEntryVO(
              entry.getPaperItemNo(), entry.getScore(), entry.getDeduction(), entry.getRemark()))
          .toList();
      progress.add(new CompetitionPracticalQuestionProgressVO(
          questionId,
          CompetitionPracticalQuestionStatuses.ofQuestion(sheetStatus, questionEntries),
          questionEntries.isEmpty() ? null : score,
          entryVos));
    }
    return progress;
  }

  private List<Long> currentRefereeMemberIds() {
    return currentRefereeMemberIds(currentProfile());
  }

  private CompetitionPracticalJudgeGroupEntity group(Long groupId) {
    CompetitionPracticalJudgeGroupEntity group = groupMapper.selectById(groupId);
    if (group == null || !Objects.equals(group.getTenantId(), tenantId())
        || !Objects.equals(group.getDeleted(), 0)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "裁判组不存在");
    }
    return group;
  }

  private CompetitionPracticalJudgeGroupEntity lockGroup(Long groupId) {
    CompetitionPracticalJudgeGroupEntity group = groupMapper.selectOne(
        new LambdaQueryWrapper<CompetitionPracticalJudgeGroupEntity>()
            .eq(CompetitionPracticalJudgeGroupEntity::getTenantId, tenantId())
            .eq(CompetitionPracticalJudgeGroupEntity::getId, groupId)
            .eq(CompetitionPracticalJudgeGroupEntity::getDeleted, 0)
            .last("FOR UPDATE"));
    if (group == null) throw new BusinessException(ErrorCode.NOT_FOUND, "裁判组不存在");
    return group;
  }

  private List<CompetitionPracticalAssignmentEntity> candidateAssignments(
      CompetitionPracticalAssignmentEntity assignment) {
    return safe(assignmentMapper.selectList(
        new LambdaQueryWrapper<CompetitionPracticalAssignmentEntity>()
            .eq(CompetitionPracticalAssignmentEntity::getTenantId, tenantId())
            .eq(CompetitionPracticalAssignmentEntity::getJudgeGroupId, assignment.getJudgeGroupId())
            .eq(CompetitionPracticalAssignmentEntity::getContestantMemberId,
                assignment.getContestantMemberId())
            .ne(CompetitionPracticalAssignmentEntity::getStatus, "withdrawn")
            .eq(CompetitionPracticalAssignmentEntity::getDeleted, 0)
            .orderByAsc(CompetitionPracticalAssignmentEntity::getId)));
  }

  private List<CompetitionPracticalAssignmentEntity> lockCandidateAssignments(
      CompetitionPracticalAssignmentEntity assignment) {
    List<CompetitionPracticalAssignmentEntity> rows = assignmentMapper.selectList(
        new LambdaQueryWrapper<CompetitionPracticalAssignmentEntity>()
            .eq(CompetitionPracticalAssignmentEntity::getTenantId, tenantId())
            .eq(CompetitionPracticalAssignmentEntity::getJudgeGroupId, assignment.getJudgeGroupId())
            .eq(CompetitionPracticalAssignmentEntity::getContestantMemberId,
                assignment.getContestantMemberId())
            .ne(CompetitionPracticalAssignmentEntity::getStatus, "withdrawn")
            .eq(CompetitionPracticalAssignmentEntity::getDeleted, 0)
            .orderByAsc(CompetitionPracticalAssignmentEntity::getId)
            .last("FOR UPDATE"));
    return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
  }

  private String startStatus(List<CompetitionPracticalAssignmentEntity> assignments) {
    if (assignments.size() == REQUIRED_JUDGES
        && assignments.stream().allMatch(item -> "completed".equals(item.getStatus()))) {
      return "completed";
    }
    if (assignments.stream().anyMatch(item -> Set.of("in_progress", "completed").contains(item.getStatus()))) {
      return "started";
    }
    long confirmed = assignments.stream().filter(item -> item.getStartConfirmedAt() != null).count();
    return assignments.size() == REQUIRED_JUDGES && confirmed == REQUIRED_JUDGES ? "ready" : "waiting";
  }

  private void requireStarted(CompetitionPracticalAssignmentEntity assignment) {
    if (!"in_progress".equals(assignment.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "组长尚未统一开考，当前不能评分");
    }
    if (assignment.getExamEndedAt() != null) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操考试已结束，不能继续评分");
    }
    if (assignment.getDeadlineAt() != null
        && !LocalDateTime.now().isBefore(assignment.getDeadlineAt())) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操考试已达到自动结束时限，不能继续评分");
    }
  }

  private CompetitionPracticalScoreSheetEntity scoreSheet(Long assignmentId) {
    return scoreSheetMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalScoreSheetEntity>()
        .eq(CompetitionPracticalScoreSheetEntity::getTenantId, tenantId())
        .eq(CompetitionPracticalScoreSheetEntity::getAssignmentId, assignmentId)
        .eq(CompetitionPracticalScoreSheetEntity::getDeleted, 0)
        .last("LIMIT 1"));
  }

  private ScoringLookup loadScoringLookup(
      List<CompetitionPracticalAssignmentEntity> assignments, JudgeTaskDisplayLookup lookup) {
    List<CompetitionPracticalAssignmentEntity> visible = safe(assignments);
    Map<Long, CompetitionPracticalJudgeGroupEntity> groups = new LinkedHashMap<>();
    Set<Long> groupIds = new LinkedHashSet<>();
    Set<Long> contestantIds = new LinkedHashSet<>();
    Set<Long> assessmentIds = new LinkedHashSet<>();
    for (CompetitionPracticalAssignmentEntity assignment : visible) {
      if (assignment.getJudgeGroupId() != null) groupIds.add(assignment.getJudgeGroupId());
      if (assignment.getContestantMemberId() != null) contestantIds.add(assignment.getContestantMemberId());
      if (assignment.getAssessmentSheetId() != null) assessmentIds.add(assignment.getAssessmentSheetId());
    }
    for (Long groupId : groupIds) {
      groups.put(groupId, group(groupId));
    }
    List<CompetitionPracticalAssignmentEntity> candidates = groupIds.isEmpty() || contestantIds.isEmpty()
        ? List.of()
        : safe(assignmentMapper.selectList(new LambdaQueryWrapper<CompetitionPracticalAssignmentEntity>()
            .eq(CompetitionPracticalAssignmentEntity::getTenantId, tenantId())
            .in(CompetitionPracticalAssignmentEntity::getJudgeGroupId, groupIds)
            .in(CompetitionPracticalAssignmentEntity::getContestantMemberId, contestantIds)
            .ne(CompetitionPracticalAssignmentEntity::getStatus, "withdrawn")
            .eq(CompetitionPracticalAssignmentEntity::getDeleted, 0)
            .orderByAsc(CompetitionPracticalAssignmentEntity::getId)));
    Map<String, List<CompetitionPracticalAssignmentEntity>> candidatesByKey = new LinkedHashMap<>();
    Set<Long> assignmentIds = new LinkedHashSet<>();
    for (CompetitionPracticalAssignmentEntity assignment : visible) {
      if (assignment.getId() != null) assignmentIds.add(assignment.getId());
    }
    for (CompetitionPracticalAssignmentEntity assignment : candidates) {
      if (assignment.getId() != null) assignmentIds.add(assignment.getId());
      candidatesByKey.computeIfAbsent(candidateKey(assignment), ignored -> new ArrayList<>()).add(assignment);
    }
    Map<Long, CompetitionPracticalScoreSheetEntity> sheetsByAssignmentId = new LinkedHashMap<>();
    if (!assignmentIds.isEmpty()) {
      List<CompetitionPracticalScoreSheetEntity> loadedSheets = scoreSheetMapper.selectList(
          new LambdaQueryWrapper<CompetitionPracticalScoreSheetEntity>()
              .eq(CompetitionPracticalScoreSheetEntity::getTenantId, tenantId())
              .in(CompetitionPracticalScoreSheetEntity::getAssignmentId, assignmentIds)
              .eq(CompetitionPracticalScoreSheetEntity::getDeleted, 0)
              .orderByDesc(CompetitionPracticalScoreSheetEntity::getRevisionNo));
      for (CompetitionPracticalScoreSheetEntity sheet : safe(loadedSheets)) {
        if (sheet != null && sheet.getAssignmentId() != null) {
          sheetsByAssignmentId.putIfAbsent(sheet.getAssignmentId(), sheet);
        }
      }
      for (Long assignmentId : assignmentIds) {
        if (sheetsByAssignmentId.containsKey(assignmentId)) continue;
        CompetitionPracticalScoreSheetEntity sheet = scoreSheet(assignmentId);
        if (sheet != null && sheet.getAssignmentId() != null) {
          sheetsByAssignmentId.putIfAbsent(sheet.getAssignmentId(), sheet);
        }
      }
    }
    Set<Long> sheetIds = new LinkedHashSet<>();
    for (CompetitionPracticalAssignmentEntity assignment : visible) {
      CompetitionPracticalScoreSheetEntity sheet = sheetsByAssignmentId.get(assignment.getId());
      if (sheet != null && sheet.getId() != null) sheetIds.add(sheet.getId());
    }
    Map<Long, List<CompetitionPracticalScoreEntryEntity>> entriesBySheetId =
        executionService.entriesBySheetIds(tenantId(), sheetIds);
    if (entriesBySheetId == null) {
      entriesBySheetId = new LinkedHashMap<>();
      for (Long sheetId : sheetIds) {
        List<CompetitionPracticalScoreEntryEntity> rows = executionService.entries(tenantId(), sheetId);
        entriesBySheetId.put(sheetId, rows == null ? List.of() : rows);
      }
    }
    Map<Long, CompetitionPracticalAssessmentSheetVO> assessments = new LinkedHashMap<>();
    for (Long assessmentId : assessmentIds) {
      CompetitionPracticalAssessmentSheetEntity assessment = assessmentSheetMapper.selectById(assessmentId);
      if (assessment != null) assessments.put(assessmentId, assessmentSheetService.toVO(assessment));
    }
    Map<Long, BigDecimal> limitsBySnapshot = new HashMap<>();
    Set<Long> confirmedContestantIds = new LinkedHashSet<>();
    Map<ScoreScope, Set<Long>> completedContestantsByScope = new LinkedHashMap<>();
    for (CompetitionPracticalAssignmentEntity assignment : visible) {
      CompetitionPracticalSessionEntity session = assignment.getSessionId() == null
          ? null : lookup.sessions().get(assignment.getSessionId());
      Long snapshotId = session == null ? null : session.getPaperSnapshotId();
      if (snapshotId != null) {
        limitsBySnapshot.computeIfAbsent(snapshotId,
            id -> executionService.paperSnapshotScoreDifference(tenantId(), id));
      }
      if ("completed".equals(startStatus(candidatesByKey.getOrDefault(candidateKey(assignment), List.of(assignment))))
          && assignment.getContestantMemberId() != null) {
        if (session != null) {
          completedContestantsByScope
              .computeIfAbsent(new ScoreScope(
                  session.getEventId(), session.getStageId(),
                  session.getEventWorkTypeId(), session.getEventWorkTypeLevelId()), ignored -> new LinkedHashSet<>())
              .add(assignment.getContestantMemberId());
        } else if (executionService.isPracticalScoreCompleted(tenantId(), assignment.getId())) {
          confirmedContestantIds.add(assignment.getContestantMemberId());
        }
      }
    }
    for (Map.Entry<ScoreScope, Set<Long>> entry : completedContestantsByScope.entrySet()) {
      Set<Long> confirmed = executionService.confirmedPracticalContestantIds(
          tenantId(), entry.getKey().eventId(), entry.getKey().stageId(),
          entry.getKey().workTypeId(), entry.getKey().levelId(), entry.getValue());
      if (confirmed != null) confirmedContestantIds.addAll(confirmed);
    }
    return new ScoringLookup(groups, candidatesByKey, sheetsByAssignmentId, entriesBySheetId,
        assessments, limitsBySnapshot, confirmedContestantIds, lookup);
  }

  private static String candidateKey(CompetitionPracticalAssignmentEntity assignment) {
    return assignment.getJudgeGroupId() + ":" + assignment.getContestantMemberId();
  }

  private record ScoreScope(Long eventId, Long stageId, Long workTypeId, Long levelId) { }

  private final class ScoringLookup {
    private final Map<Long, CompetitionPracticalJudgeGroupEntity> groups;
    private final Map<String, List<CompetitionPracticalAssignmentEntity>> candidatesByKey;
    private final Map<Long, CompetitionPracticalScoreSheetEntity> sheetsByAssignmentId;
    private final Map<Long, List<CompetitionPracticalScoreEntryEntity>> entriesBySheetId;
    private final Map<Long, CompetitionPracticalAssessmentSheetVO> assessments;
    private final Map<Long, BigDecimal> limitsBySnapshot;
    private final Set<Long> confirmedContestantIds;
    private final JudgeTaskDisplayLookup lookup;

    private ScoringLookup(
        Map<Long, CompetitionPracticalJudgeGroupEntity> groups,
        Map<String, List<CompetitionPracticalAssignmentEntity>> candidatesByKey,
        Map<Long, CompetitionPracticalScoreSheetEntity> sheetsByAssignmentId,
        Map<Long, List<CompetitionPracticalScoreEntryEntity>> entriesBySheetId,
        Map<Long, CompetitionPracticalAssessmentSheetVO> assessments,
        Map<Long, BigDecimal> limitsBySnapshot,
        Set<Long> confirmedContestantIds,
        JudgeTaskDisplayLookup lookup) {
      this.groups = groups;
      this.candidatesByKey = candidatesByKey;
      this.sheetsByAssignmentId = sheetsByAssignmentId;
      this.entriesBySheetId = entriesBySheetId;
      this.assessments = assessments;
      this.limitsBySnapshot = limitsBySnapshot;
      this.confirmedContestantIds = confirmedContestantIds;
      this.lookup = lookup;
    }

    private CompetitionPracticalJudgeGroupEntity groupOf(Long groupId) {
      CompetitionPracticalJudgeGroupEntity group = groups.get(groupId);
      return group == null ? group(groupId) : group;
    }

    private List<CompetitionPracticalAssignmentEntity> candidatesOf(
        CompetitionPracticalAssignmentEntity assignment) {
      List<CompetitionPracticalAssignmentEntity> rows = candidatesByKey.get(candidateKey(assignment));
      return rows == null || rows.isEmpty() ? List.of(assignment) : rows;
    }

    private CompetitionPracticalScoreSheetEntity sheetOf(Long assignmentId) {
      return assignmentId == null ? null : sheetsByAssignmentId.get(assignmentId);
    }

    private List<CompetitionPracticalScoreEntryEntity> entriesOf(Long sheetId) {
      return sheetId == null ? List.of() : entriesBySheetId.getOrDefault(sheetId, List.of());
    }

    private CompetitionPracticalAssessmentSheetVO assessmentOf(Long assessmentSheetId) {
      return assessmentSheetId == null ? null : assessments.get(assessmentSheetId);
    }

    private CompetitionPracticalExecutionService.PracticalScoreDifferencePreview previewOf(
        CompetitionPracticalAssignmentEntity assignment, List<BigDecimal> submittedScores) {
      CompetitionPracticalSessionEntity session = assignment.getSessionId() == null
          ? null : lookup.sessions().get(assignment.getSessionId());
      Long snapshotId = session == null ? null : session.getPaperSnapshotId();
      BigDecimal limit = snapshotId == null ? null : limitsBySnapshot.get(snapshotId);
      CompetitionPracticalExecutionService.PracticalScoreDifferencePreview preview =
          executionService.previewPracticalScoreDifference(limit, submittedScores);
      return preview == null
          ? new CompetitionPracticalExecutionService.PracticalScoreDifferencePreview(
              null, null, 0, "waiting", false)
          : preview;
    }

    private boolean practicalCompleted(Long contestantMemberId) {
      return contestantMemberId != null && confirmedContestantIds.contains(contestantMemberId);
    }
  }

  private JudgeTaskDisplayLookup loadDisplayLookup(
      Long tenantId, List<CompetitionPracticalAssignmentEntity> assignments) {
    Set<Long> sessionIds = new LinkedHashSet<>();
    Set<Long> contestantIds = new LinkedHashSet<>();
    for (CompetitionPracticalAssignmentEntity assignment : safe(assignments)) {
      if (assignment.getSessionId() != null) sessionIds.add(assignment.getSessionId());
      if (assignment.getContestantMemberId() != null) contestantIds.add(assignment.getContestantMemberId());
    }
    Map<Long, CompetitionPracticalSessionEntity> sessions = sessionIds.isEmpty() ? Map.of()
        : safe(sessionMapper.selectList(new LambdaQueryWrapper<CompetitionPracticalSessionEntity>()
            .eq(CompetitionPracticalSessionEntity::getTenantId, tenantId)
            .in(CompetitionPracticalSessionEntity::getId, sessionIds)
            .eq(CompetitionPracticalSessionEntity::getDeleted, 0))).stream()
            .filter(item -> item != null && item.getId() != null)
            .collect(java.util.stream.Collectors.toMap(
                CompetitionPracticalSessionEntity::getId, item -> item, (left, right) -> left,
                LinkedHashMap::new));
    Set<Long> stageIds = new LinkedHashSet<>();
    Set<Long> eventIds = new LinkedHashSet<>();
    Set<Long> workTypeIds = new LinkedHashSet<>();
    Set<Long> levelIds = new LinkedHashSet<>();
    for (CompetitionPracticalSessionEntity session : sessions.values()) {
      if (session.getStageId() != null) stageIds.add(session.getStageId());
      if (session.getEventId() != null) eventIds.add(session.getEventId());
      if (session.getEventWorkTypeId() != null) workTypeIds.add(session.getEventWorkTypeId());
      if (session.getEventWorkTypeLevelId() != null) levelIds.add(session.getEventWorkTypeLevelId());
    }
    Map<Long, CompetitionStageEntity> stages = stageIds.isEmpty() ? Map.of()
        : safe(stageMapper.selectList(new LambdaQueryWrapper<CompetitionStageEntity>()
            .eq(CompetitionStageEntity::getTenantId, tenantId)
            .in(CompetitionStageEntity::getId, stageIds)
            .eq(CompetitionStageEntity::getDeleted, 0))).stream()
            .filter(item -> item != null && item.getId() != null)
            .collect(java.util.stream.Collectors.toMap(
                CompetitionStageEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    Map<Long, CompetitionEventEntity> events = eventIds.isEmpty() ? Map.of()
        : safe(eventMapper.selectList(new LambdaQueryWrapper<CompetitionEventEntity>()
            .eq(CompetitionEventEntity::getTenantId, tenantId)
            .in(CompetitionEventEntity::getId, eventIds)
            .eq(CompetitionEventEntity::getDeleted, 0))).stream()
            .filter(item -> item != null && item.getId() != null)
            .collect(java.util.stream.Collectors.toMap(
                CompetitionEventEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    Map<Long, CompetitionEventWorkTypeEntity> workTypes = workTypeIds.isEmpty() ? Map.of()
        : safe(eventWorkTypeMapper.selectList(new LambdaQueryWrapper<CompetitionEventWorkTypeEntity>()
            .eq(CompetitionEventWorkTypeEntity::getTenantId, tenantId)
            .in(CompetitionEventWorkTypeEntity::getId, workTypeIds)
            .eq(CompetitionEventWorkTypeEntity::getDeleted, 0))).stream()
            .filter(item -> item != null && item.getId() != null)
            .collect(java.util.stream.Collectors.toMap(
                CompetitionEventWorkTypeEntity::getId, item -> item, (left, right) -> left,
                LinkedHashMap::new));
    Map<Long, CompetitionEventWorkTypeLevelEntity> levels = levelIds.isEmpty() ? Map.of()
        : safe(eventWorkTypeLevelMapper.selectList(
            new LambdaQueryWrapper<CompetitionEventWorkTypeLevelEntity>()
                .eq(CompetitionEventWorkTypeLevelEntity::getTenantId, tenantId)
                .in(CompetitionEventWorkTypeLevelEntity::getId, levelIds)
                .eq(CompetitionEventWorkTypeLevelEntity::getDeleted, 0))).stream()
            .filter(item -> item != null && item.getId() != null)
            .collect(java.util.stream.Collectors.toMap(
                CompetitionEventWorkTypeLevelEntity::getId, item -> item, (left, right) -> left,
                LinkedHashMap::new));
    Map<Long, String> contestantNos = new LinkedHashMap<>();
    Map<Long, Long> registrationIds = new LinkedHashMap<>();
    if (!contestantIds.isEmpty()) {
      for (CompetitionContestantProfileEntity profile : safe(contestantProfileMapper.selectList(
          new LambdaQueryWrapper<CompetitionContestantProfileEntity>()
              .eq(CompetitionContestantProfileEntity::getTenantId, tenantId)
              .in(CompetitionContestantProfileEntity::getMemberId, contestantIds)
              .eq(CompetitionContestantProfileEntity::getDeleted, 0)))) {
        if (profile == null || profile.getMemberId() == null) continue;
        if (StringUtils.hasText(profile.getContestantNo())) {
          contestantNos.putIfAbsent(profile.getMemberId(), profile.getContestantNo().trim());
        }
        if (profile.getRegistrationId() != null) {
          registrationIds.putIfAbsent(profile.getMemberId(), profile.getRegistrationId());
        }
      }
    }
    Map<Long, Long> rosterIdByRegistration = new LinkedHashMap<>();
    if (!registrationIds.isEmpty()) {
      for (CompetitionRegistrationEntity registration : safe(registrationMapper.selectList(
          new LambdaQueryWrapper<CompetitionRegistrationEntity>()
              .eq(CompetitionRegistrationEntity::getTenantId, tenantId)
              .in(CompetitionRegistrationEntity::getId, registrationIds.values())
              .eq(CompetitionRegistrationEntity::getDeleted, 0)))) {
        if (registration != null && registration.getId() != null && registration.getRosterItemId() != null) {
          rosterIdByRegistration.putIfAbsent(registration.getId(), registration.getRosterItemId());
        }
      }
    }
    Map<Long, String> rosterNames = new LinkedHashMap<>();
    if (!rosterIdByRegistration.isEmpty()) {
      for (CompetitionRosterItemEntity roster : safe(rosterItemMapper.selectList(
          new LambdaQueryWrapper<CompetitionRosterItemEntity>()
              .eq(CompetitionRosterItemEntity::getTenantId, tenantId)
              .in(CompetitionRosterItemEntity::getId, rosterIdByRegistration.values())
              .eq(CompetitionRosterItemEntity::getDeleted, 0)))) {
        if (roster != null && roster.getId() != null && StringUtils.hasText(roster.getName())) {
          rosterNames.putIfAbsent(roster.getId(), roster.getName().trim());
        }
      }
    }
    Map<Long, String> contestantNames = new LinkedHashMap<>();
    registrationIds.forEach((memberId, registrationId) -> {
      Long rosterId = rosterIdByRegistration.get(registrationId);
      String name = rosterId == null ? null : rosterNames.get(rosterId);
      if (StringUtils.hasText(name)) contestantNames.put(memberId, name);
    });
    return new JudgeTaskDisplayLookup(sessions, stages, events, workTypes, levels,
        contestantNos, contestantNames);
  }

  private record JudgeTaskDisplay(
      Long eventId, String eventName, Long stageId, String stageName,
      String workTypeName, String levelName,
      String contestantNo, String contestantName) {
  }

  private record JudgeTaskDisplayLookup(
      Map<Long, CompetitionPracticalSessionEntity> sessions,
      Map<Long, CompetitionStageEntity> stages,
      Map<Long, CompetitionEventEntity> events,
      Map<Long, CompetitionEventWorkTypeEntity> workTypes,
      Map<Long, CompetitionEventWorkTypeLevelEntity> levels,
      Map<Long, String> contestantNos,
      Map<Long, String> contestantNames) {
    private JudgeTaskDisplay displayOf(CompetitionPracticalAssignmentEntity assignment) {
      CompetitionPracticalSessionEntity session = assignment.getSessionId() == null
          ? null : sessions.get(assignment.getSessionId());
      Long stageId = session == null ? null : session.getStageId();
      Long eventId = session == null ? null : session.getEventId();
      Long workTypeId = session == null ? null : session.getEventWorkTypeId();
      Long levelId = session == null ? null : session.getEventWorkTypeLevelId();
      CompetitionStageEntity stage = stageId == null ? null : stages.get(stageId);
      CompetitionEventEntity event = eventId == null ? null : events.get(eventId);
      CompetitionEventWorkTypeEntity workType = workTypeId == null ? null : workTypes.get(workTypeId);
      CompetitionEventWorkTypeLevelEntity level = levelId == null ? null : levels.get(levelId);
      return new JudgeTaskDisplay(
          eventId,
          event == null ? null : event.getName(),
          stageId,
          stage == null ? null : stage.getName(),
          workType == null || !StringUtils.hasText(workType.getDisplayName())
              ? null : workType.getDisplayName().trim(),
          levelName(level),
          contestantNos.get(assignment.getContestantMemberId()),
          contestantNames.get(assignment.getContestantMemberId()));
    }

    private static String levelName(CompetitionEventWorkTypeLevelEntity level) {
      if (level == null) return null;
      if (StringUtils.hasText(level.getLevelName())) return level.getLevelName().trim();
      return StringUtils.hasText(level.getLevelCode()) ? level.getLevelCode().trim() : null;
    }
  }

  private List<CompetitionPracticalAssignmentEntity> currentAssignments(
      CompetitionRefereeProfileEntity profile) {
    List<Long> memberIds = currentRefereeMemberIds(profile);
    if (memberIds.isEmpty()) return List.of();
    return safe(assignmentMapper.selectList(
        new LambdaQueryWrapper<CompetitionPracticalAssignmentEntity>()
            .eq(CompetitionPracticalAssignmentEntity::getTenantId, tenantId())
            .in(CompetitionPracticalAssignmentEntity::getRefereeMemberId, memberIds)
            .eq(CompetitionPracticalAssignmentEntity::getDeleted, 0)
            .orderByAsc(CompetitionPracticalAssignmentEntity::getAssignedAt)));
  }

  private List<Long> currentRefereeMemberIds(CompetitionRefereeProfileEntity profile) {
    return safe(eventMemberMapper.selectList(
        new LambdaQueryWrapper<CompetitionEventMemberEntity>()
            .eq(CompetitionEventMemberEntity::getTenantId, tenantId())
            .eq(CompetitionEventMemberEntity::getPersonId, profile.getPersonId())
            .eq(CompetitionEventMemberEntity::getRoleType, "referee")
            .eq(CompetitionEventMemberEntity::getStatus, "active")
            .eq(CompetitionEventMemberEntity::getDeleted, 0)))
        .stream().map(CompetitionEventMemberEntity::getId).toList();
  }

  private CompetitionRefereeGroupEntity currentConfiguredGroup(
      CompetitionRefereeProfileEntity profile) {
    if (profile == null || profile.getId() == null) return null;
    CompetitionRefereeGroupMemberEntity membership = configuredGroupMemberMapper.selectOne(
        new LambdaQueryWrapper<CompetitionRefereeGroupMemberEntity>()
            .eq(CompetitionRefereeGroupMemberEntity::getTenantId, tenantId())
            .eq(CompetitionRefereeGroupMemberEntity::getRefereeProfileId, profile.getId())
            .eq(CompetitionRefereeGroupMemberEntity::getDeleted, 0)
            .eq(CompetitionRefereeGroupMemberEntity::getStatus, "active")
            .last("LIMIT 1"));
    if (membership == null || membership.getGroupId() == null) return null;
    CompetitionRefereeGroupEntity group = configuredGroupMapper.selectById(membership.getGroupId());
    if (group == null || !Objects.equals(group.getTenantId(), tenantId())
        || !Objects.equals(group.getDeleted(), 0)) {
      return null;
    }
    return group;
  }

  private CompetitionRefereeGroupEntity lockCurrentConfiguredGroup(
      CompetitionRefereeProfileEntity profile) {
    CompetitionRefereeGroupEntity current = currentConfiguredGroup(profile);
    if (current == null) return null;
    CompetitionRefereeGroupEntity locked = configuredGroupMapper.selectOne(
        new LambdaQueryWrapper<CompetitionRefereeGroupEntity>()
            .eq(CompetitionRefereeGroupEntity::getTenantId, tenantId())
            .eq(CompetitionRefereeGroupEntity::getId, current.getId())
            .eq(CompetitionRefereeGroupEntity::getDeleted, 0)
            .last("FOR UPDATE"));
    if (locked == null) throw new BusinessException(ErrorCode.NOT_FOUND, "裁判小组不存在");
    return locked;
  }

  private boolean isConfiguredGroupLeader(
      CompetitionRefereeGroupEntity group, CompetitionRefereeProfileEntity profile) {
    if (group == null || profile == null) return false;
    if (profile.getId() != null && Objects.equals(group.getLeaderRefereeProfileId(), profile.getId())) {
      return true;
    }
    return group.getLeaderRefereeMemberId() != null
        && currentRefereeMemberIds(profile).contains(group.getLeaderRefereeMemberId());
  }

  private Long resolveActiveStageId(
      CompetitionRefereeGroupEntity group,
      List<CompetitionPracticalAssignmentEntity> assignments,
      JudgeTaskDisplayLookup lookup) {
    Set<Long> available = availableStageIds(assignments, lookup);
    if (group != null && group.getActiveStageId() != null && available.contains(group.getActiveStageId())) {
      return group.getActiveStageId();
    }
    return available.stream()
        .sorted(Comparator
            .comparing((Long id) -> {
              CompetitionStageEntity stage = lookup.stages().get(id);
              return stage == null || stage.getSortOrder() == null ? Integer.MAX_VALUE : stage.getSortOrder();
            })
            .thenComparing(id -> id))
        .findFirst()
        .orElse(group == null ? null : group.getActiveStageId());
  }

  private Set<Long> availableStageIds(
      List<CompetitionPracticalAssignmentEntity> assignments, JudgeTaskDisplayLookup lookup) {
    Set<Long> stageIds = new LinkedHashSet<>();
    for (CompetitionPracticalAssignmentEntity assignment : safe(assignments)) {
      Long stageId = lookup.displayOf(assignment).stageId();
      if (stageId != null) stageIds.add(stageId);
    }
    return stageIds;
  }

  private Long parseStageId(String stageIdText) {
    if (!StringUtils.hasText(stageIdText)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛段不能为空");
    }
    try {
      return Long.parseLong(stageIdText.trim());
    } catch (NumberFormatException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛段无效");
    }
  }

  private Long tenantId() { return SecurityContext.currentUser().getTenantId(); }
  private Long userId() { return SecurityContext.currentUser().getUserId(); }
  private void publish(String type, Long assignmentId) {
    if (realtimePublisher != null) realtimePublisher.publishAfterCommit(tenantId(), type, assignmentId);
  }
  private static String id(Long value) { return value == null ? null : String.valueOf(value); }
  private static <T> List<T> safe(List<T> values) {
    return values == null ? new ArrayList<>() : new ArrayList<>(values);
  }
}
