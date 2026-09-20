package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.domain.CompetitionPracticalQuestionStatuses;
import com.guoyu.platform.competition.dto.CompetitionPracticalScoreSaveReq;
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalAssessmentSheetEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalJudgeGroupEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreEntryEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreSheetEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeProfileEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalAssessmentSheetMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalJudgeGroupMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalScoreSheetMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeProfileMapper;
import com.guoyu.platform.competition.vo.CompetitionPracticalAssessmentSheetVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalJudgeScoreEntryVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalJudgeTaskVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalQuestionProgressVO;
import com.guoyu.platform.security.SecurityContext;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import com.guoyu.platform.competition.realtime.CompetitionPracticalRealtimePublisher;

@Service
public class CompetitionPracticalJudgeService {
  private static final Logger log = LoggerFactory.getLogger(CompetitionPracticalJudgeService.class);
  private static final int REQUIRED_JUDGES = 3;
  private final CompetitionRefereeProfileMapper profileMapper;
  private final CompetitionEventMemberMapper eventMemberMapper;
  private final CompetitionPracticalAssignmentMapper assignmentMapper;
  private final CompetitionPracticalJudgeGroupMapper groupMapper;
  private final CompetitionPracticalAssessmentSheetMapper assessmentSheetMapper;
  private final CompetitionPracticalScoreSheetMapper scoreSheetMapper;
  private final CompetitionPracticalAssessmentSheetService assessmentSheetService;
  private final CompetitionPracticalExecutionService executionService;
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
      CompetitionPracticalAssessmentSheetMapper assessmentSheetMapper,
      CompetitionPracticalScoreSheetMapper scoreSheetMapper,
      CompetitionPracticalAssessmentSheetService assessmentSheetService,
      CompetitionPracticalExecutionService executionService) {
    this.profileMapper = profileMapper;
    this.eventMemberMapper = eventMemberMapper;
    this.assignmentMapper = assignmentMapper;
    this.groupMapper = groupMapper;
    this.assessmentSheetMapper = assessmentSheetMapper;
    this.scoreSheetMapper = scoreSheetMapper;
    this.assessmentSheetService = assessmentSheetService;
    this.executionService = executionService;
  }

  @Transactional(readOnly = true)
  public List<CompetitionPracticalJudgeTaskVO> tasks() {
    var principal = SecurityContext.currentUser();
    CompetitionRefereeProfileEntity profile = currentProfile();
    List<CompetitionEventMemberEntity> members = safe(eventMemberMapper.selectList(
        new LambdaQueryWrapper<CompetitionEventMemberEntity>()
            .eq(CompetitionEventMemberEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionEventMemberEntity::getPersonId, profile.getPersonId())
            .eq(CompetitionEventMemberEntity::getRoleType, "referee")
            .eq(CompetitionEventMemberEntity::getStatus, "active")
            .eq(CompetitionEventMemberEntity::getDeleted, 0)));
    List<Long> memberIds = members.stream().map(CompetitionEventMemberEntity::getId).toList();
    if (memberIds.isEmpty()) return List.of();
    return safe(assignmentMapper.selectList(
        new LambdaQueryWrapper<CompetitionPracticalAssignmentEntity>()
            .eq(CompetitionPracticalAssignmentEntity::getTenantId, principal.getTenantId())
            .in(CompetitionPracticalAssignmentEntity::getRefereeMemberId, memberIds)
            .eq(CompetitionPracticalAssignmentEntity::getDeleted, 0)
            .orderByAsc(CompetitionPracticalAssignmentEntity::getAssignedAt)))
        .stream().map(this::toVO).toList();
  }

  @Transactional
  public CompetitionPracticalJudgeTaskVO open(Long assignmentId) {
    CompetitionPracticalAssignmentEntity assignment = ownedAssignment(assignmentId);
    requireStarted(assignment);
    executionService.openSheet(tenantId(), userId(), assignmentId);
    publish("score-sheet-opened", assignmentId);
    return toVO(assignment);
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
    publish(submit ? "score-sheet-submitted" : "score-sheet-saved", assignmentId);
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
      return toVO(assignment);
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
    return toVO(assignment);
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
      return toVO(owned);
    }
    long confirmed = assignments.stream()
        .filter(item -> item.getStartConfirmedAt() != null)
        .count();
    if (confirmed != REQUIRED_JUDGES) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "三名裁判必须分别在各自设备确认后，组长才能开始考试");
    }
    executionService.ensureInteractiveStage(tenantId(), userId(), owned.getSessionId());
    LocalDateTime now = LocalDateTime.now();
    Long currentUserId = userId();
    for (CompetitionPracticalAssignmentEntity assignment : assignments) {
      assignment.setStatus("in_progress");
      assignment.setStartedAt(now);
      assignment.setStartedBy(currentUserId);
      assignment.setUpdatedBy(currentUserId);
      if (assignmentMapper.updateById(assignment) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "统一开考状态已变化，请刷新后重试");
      }
    }
    publish("practical-session-started", assignmentId);
    return toVO(owned);
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
    CompetitionPracticalAssessmentSheetEntity assessment = assignment.getAssessmentSheetId() == null
        ? null : assessmentSheetMapper.selectById(assignment.getAssessmentSheetId());
    CompetitionPracticalScoreSheetEntity score = scoreSheet(assignment.getId());
    CompetitionPracticalJudgeGroupEntity group = group(assignment.getJudgeGroupId());
    List<CompetitionPracticalAssignmentEntity> groupAssignments = candidateAssignments(assignment);
    int confirmedJudges = (int) groupAssignments.stream()
        .filter(item -> item.getStartConfirmedAt() != null)
        .count();
    CompetitionPracticalAssessmentSheetVO assessmentVo = assessment == null
        ? null : assessmentSheetService.toVO(assessment);
    List<CompetitionPracticalScoreEntryEntity> entries = score == null
        ? List.of() : executionService.entries(tenantId(), score.getId());
    return new CompetitionPracticalJudgeTaskVO(id(assignment.getId()), id(assignment.getSessionId()),
        id(assignment.getContestantMemberId()), id(assignment.getRefereeMemberId()),
        assignment.getWorkstationCode(), assignment.getStatus(),
        id(assignment.getAssessmentSheetId()), assessmentVo, id(score == null ? null : score.getId()),
        score == null ? null : score.getStatus(), score == null ? null : score.getTotalScore(),
        Objects.equals(group.getLeaderRefereeMemberId(), assignment.getRefereeMemberId())
            ? "leader" : "member",
        startStatus(groupAssignments), confirmedJudges, REQUIRED_JUDGES,
        assignment.getStartConfirmedAt() != null,
        questionProgress(assessmentVo, score, entries));
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
    var principal = SecurityContext.currentUser();
    CompetitionRefereeProfileEntity profile = currentProfile();
    return safe(eventMemberMapper.selectList(
        new LambdaQueryWrapper<CompetitionEventMemberEntity>()
            .eq(CompetitionEventMemberEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionEventMemberEntity::getPersonId, profile.getPersonId())
            .eq(CompetitionEventMemberEntity::getRoleType, "referee")
            .eq(CompetitionEventMemberEntity::getStatus, "active")
            .eq(CompetitionEventMemberEntity::getDeleted, 0)))
        .stream().map(CompetitionEventMemberEntity::getId).toList();
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
  }

  private CompetitionPracticalScoreSheetEntity scoreSheet(Long assignmentId) {
    return scoreSheetMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalScoreSheetEntity>()
        .eq(CompetitionPracticalScoreSheetEntity::getTenantId, tenantId())
        .eq(CompetitionPracticalScoreSheetEntity::getAssignmentId, assignmentId)
        .eq(CompetitionPracticalScoreSheetEntity::getDeleted, 0)
        .last("LIMIT 1"));
  }

  private Long tenantId() { return SecurityContext.currentUser().getTenantId(); }
  private Long userId() { return SecurityContext.currentUser().getUserId(); }
  private void publish(String type, Long assignmentId) {
    if (realtimePublisher != null) realtimePublisher.publish(tenantId(), type, assignmentId);
  }
  private static String id(Long value) { return value == null ? null : String.valueOf(value); }
  private static <T> List<T> safe(List<T> values) {
    return values == null ? new ArrayList<>() : new ArrayList<>(values);
  }
}
