package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.domain.CompetitionPracticalAverageCalculator;
import com.guoyu.platform.competition.domain.CompetitionPracticalQuestionStatuses;
import com.guoyu.platform.competition.domain.CompetitionPracticalRuleDomainService;
import com.guoyu.platform.competition.domain.CompetitionStageLifecycleGuard;
import com.guoyu.platform.competition.dto.CompetitionPracticalAssignmentReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalJudgeGroupReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalJudgeMemberReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalScoreEntryReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalScoreSaveReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalSessionCreateReq;
import com.guoyu.platform.competition.entity.CompetitionPracticalAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalJudgeGroupEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalJudgeMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalRuleEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalRuleItemEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreEntryEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreSheetEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeAvoidanceEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.entity.CompetitionStageExecutionPlanEntity;
import com.guoyu.platform.competition.enums.CompetitionPracticalScoreStatus;
import com.guoyu.platform.competition.enums.CompetitionScoreSourceType;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalJudgeGroupMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalJudgeMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalRuleItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalRuleMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalScoreEntryMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalScoreMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalScoreSheetMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPaperQuestionMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeAvoidanceMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreRuleMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreSourceMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageExecutionPlanMapper;
import com.guoyu.platform.competition.messaging.service.CompetitionScorePipelineOutboxService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetitionPracticalExecutionService {
  private static final Logger log = LoggerFactory.getLogger(CompetitionPracticalExecutionService.class);
  private enum ScorePersistMode { DRAFT, SUBMIT_QUESTION, LOCK_ALL }
  private final CompetitionPracticalSessionMapper sessionMapper;
  private final CompetitionPracticalJudgeGroupMapper groupMapper;
  private final CompetitionPracticalJudgeMemberMapper memberMapper;
  private final CompetitionPracticalAssignmentMapper assignmentMapper;
  private final CompetitionPracticalRuleMapper ruleMapper;
  private final CompetitionPracticalRuleItemMapper ruleItemMapper;
  private final CompetitionPracticalScoreSheetMapper sheetMapper;
  private final CompetitionPracticalScoreEntryMapper entryMapper;
  private final CompetitionPracticalScoreMapper practicalScoreMapper;
  private final CompetitionRefereeAvoidanceMapper avoidanceMapper;
  private final CompetitionScoreRuleMapper scoreRuleMapper;
  private final CompetitionScoreSourceMapper scoreSourceMapper;
  private final CompetitionStageExecutionPlanMapper planMapper;
  private final CompetitionStageLifecycleGuard lifecycleGuard;
  private final CompetitionMakeExamPaperQuestionMapper paperQuestionMapper;
  private final ObjectMapper objectMapper;
  private final CompetitionEventMemberMapper contestantMemberMapper;
  private final CompetitionPlayerSessionConflictDomainService sessionConflictService;
  private final CompetitionPracticalRuleDomainService ruleDomain = new CompetitionPracticalRuleDomainService();
  private final CompetitionPracticalAverageCalculator averageCalculator = new CompetitionPracticalAverageCalculator();
  private CompetitionScorePipelineOutboxService scorePipelineOutboxService;
  private CompetitionAutomaticScorePipelineService automaticScorePipelineService;
  private CompetitionStageService stageService;

  @Autowired(required = false)
  public void setScorePipelineOutboxService(
      CompetitionScorePipelineOutboxService scorePipelineOutboxService) {
    this.scorePipelineOutboxService = scorePipelineOutboxService;
  }

  @Autowired(required = false)
  public void setAutomaticScorePipelineService(
      CompetitionAutomaticScorePipelineService automaticScorePipelineService) {
    this.automaticScorePipelineService = automaticScorePipelineService;
  }

  @Autowired(required = false)
  public void setStageService(CompetitionStageService stageService) {
    this.stageService = stageService;
  }

  public CompetitionPracticalExecutionService(
      CompetitionPracticalSessionMapper sessionMapper,
      CompetitionPracticalJudgeGroupMapper groupMapper,
      CompetitionPracticalJudgeMemberMapper memberMapper,
      CompetitionPracticalAssignmentMapper assignmentMapper,
      CompetitionPracticalRuleMapper ruleMapper,
      CompetitionPracticalRuleItemMapper ruleItemMapper,
      CompetitionPracticalScoreSheetMapper sheetMapper,
      CompetitionPracticalScoreEntryMapper entryMapper,
      CompetitionPracticalScoreMapper practicalScoreMapper,
      CompetitionRefereeAvoidanceMapper avoidanceMapper,
      CompetitionScoreRuleMapper scoreRuleMapper,
      CompetitionScoreSourceMapper scoreSourceMapper,
      CompetitionStageExecutionPlanMapper planMapper,
      CompetitionStageLifecycleGuard lifecycleGuard) {
    this(sessionMapper, groupMapper, memberMapper, assignmentMapper, ruleMapper, ruleItemMapper,
        sheetMapper, entryMapper, practicalScoreMapper, avoidanceMapper, scoreRuleMapper,
        scoreSourceMapper, planMapper, lifecycleGuard, null, null, null, null);
  }

  @Autowired
  public CompetitionPracticalExecutionService(
      CompetitionPracticalSessionMapper sessionMapper,
      CompetitionPracticalJudgeGroupMapper groupMapper,
      CompetitionPracticalJudgeMemberMapper memberMapper,
      CompetitionPracticalAssignmentMapper assignmentMapper,
      CompetitionPracticalRuleMapper ruleMapper,
      CompetitionPracticalRuleItemMapper ruleItemMapper,
      CompetitionPracticalScoreSheetMapper sheetMapper,
      CompetitionPracticalScoreEntryMapper entryMapper,
      CompetitionPracticalScoreMapper practicalScoreMapper,
      CompetitionRefereeAvoidanceMapper avoidanceMapper,
      CompetitionScoreRuleMapper scoreRuleMapper,
      CompetitionScoreSourceMapper scoreSourceMapper,
      CompetitionStageExecutionPlanMapper planMapper,
      CompetitionStageLifecycleGuard lifecycleGuard,
      CompetitionMakeExamPaperQuestionMapper paperQuestionMapper,
      ObjectMapper objectMapper,
      CompetitionEventMemberMapper contestantMemberMapper,
      CompetitionPlayerSessionConflictDomainService sessionConflictService) {
    this.sessionMapper = sessionMapper;
    this.groupMapper = groupMapper;
    this.memberMapper = memberMapper;
    this.assignmentMapper = assignmentMapper;
    this.ruleMapper = ruleMapper;
    this.ruleItemMapper = ruleItemMapper;
    this.sheetMapper = sheetMapper;
    this.entryMapper = entryMapper;
    this.practicalScoreMapper = practicalScoreMapper;
    this.avoidanceMapper = avoidanceMapper;
    this.scoreRuleMapper = scoreRuleMapper;
    this.scoreSourceMapper = scoreSourceMapper;
    this.planMapper = planMapper;
    this.lifecycleGuard = lifecycleGuard;
    this.paperQuestionMapper = paperQuestionMapper;
    this.objectMapper = objectMapper;
    this.contestantMemberMapper = contestantMemberMapper;
    this.sessionConflictService = sessionConflictService;
  }

  @Transactional(readOnly = true)
  public List<CompetitionPracticalSessionEntity> sessions(Long tenantId, Long planId) {
    List<CompetitionPracticalSessionEntity> rows = sessionMapper.selectList(new LambdaQueryWrapper<CompetitionPracticalSessionEntity>()
        .eq(CompetitionPracticalSessionEntity::getTenantId, tenantId)
        .eq(CompetitionPracticalSessionEntity::getPlanId, planId)
        .eq(CompetitionPracticalSessionEntity::getDeleted, 0)
        .orderByAsc(CompetitionPracticalSessionEntity::getStartAt));
    return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
  }

  @Transactional
  public CompetitionPracticalSessionEntity createSession(Long tenantId, Long userId, Long eventId, Long stageId,
      CompetitionPracticalSessionCreateReq req, Long planWorkTypeId, Long planLevelId) {
    if (!req.endAt().isAfter(req.startAt())) throw new BusinessException(ErrorCode.BAD_REQUEST, "实操场次结束时间必须晚于开始时间");
    lifecycleGuard.requireStageAllocationWritable(lifecycleGuard.lockStage(tenantId, stageId));
    CompetitionPracticalSessionEntity existing = sessionMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalSessionEntity>()
        .eq(CompetitionPracticalSessionEntity::getTenantId, tenantId)
        .eq(CompetitionPracticalSessionEntity::getPlanId, req.planId())
        .eq(CompetitionPracticalSessionEntity::getSessionCode, req.sessionCode().trim())
        .eq(CompetitionPracticalSessionEntity::getDeleted, 0));
    if (existing != null) return existing;
    CompetitionPracticalSessionEntity row = new CompetitionPracticalSessionEntity();
    row.setId(IdWorker.getId()); row.setTenantId(tenantId); row.setPlanId(req.planId()); row.setEventId(eventId);
    row.setStageId(stageId); row.setEventWorkTypeId(planWorkTypeId); row.setEventWorkTypeLevelId(planLevelId);
    row.setSessionCode(req.sessionCode().trim()); row.setSessionName(req.sessionName().trim()); row.setStartAt(req.startAt());
    row.setEndAt(req.endAt()); row.setCapacity(req.capacity()); row.setResourceId(req.resourceId()); row.setStatus("draft");
    row.setCreatedBy(userId); row.setUpdatedBy(userId);
    if (sessionMapper.insert(row) != 1) throw new BusinessException(ErrorCode.CONFLICT, "实操场次保存失败");
    return row;
  }

  @Transactional
  public CompetitionPracticalSessionEntity bindPaperSnapshot(
      Long tenantId, Long userId, Long sessionId, Long paperSnapshotId) {
    CompetitionPracticalSessionEntity row = session(tenantId, sessionId);
    if (!"draft".equals(row.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "只有草稿实操场次可以绑定组卷快照");
    }
    row.setPaperSnapshotId(paperSnapshotId);
    row.setUpdatedBy(userId);
    if (sessionMapper.updateById(row) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操场次绑定组卷快照失败");
    }
    return row;
  }

  @Transactional
  public CompetitionPracticalSessionEntity publishSession(Long tenantId, Long sessionId) {
    CompetitionPracticalSessionEntity row = session(tenantId, sessionId);
    if (!"draft".equals(row.getStatus())) throw new BusinessException(ErrorCode.CONFLICT, "仅草稿实操场次允许发布");
    lifecycleGuard.requireStageAllocationWritable(lifecycleGuard.lockStage(tenantId, row.getStageId()));
    lifecycleGuard.requirePublishedPlan(tenantId, row.getPlanId(), row.getEventId(), row.getStageId(),
        row.getEventWorkTypeId(), row.getEventWorkTypeLevelId());
    row.setStatus("published");
    if (sessionMapper.updateById(row) != 1) throw new BusinessException(ErrorCode.CONFLICT, "实操场次发布失败");
    return row;
  }

  @Transactional(readOnly = true)
  public List<CompetitionPracticalJudgeGroupEntity> groups(Long tenantId, Long sessionId) {
    List<CompetitionPracticalJudgeGroupEntity> rows = groupMapper.selectList(new LambdaQueryWrapper<CompetitionPracticalJudgeGroupEntity>()
        .eq(CompetitionPracticalJudgeGroupEntity::getTenantId, tenantId)
        .eq(CompetitionPracticalJudgeGroupEntity::getSessionId, sessionId)
        .eq(CompetitionPracticalJudgeGroupEntity::getDeleted, 0)
        .orderByAsc(CompetitionPracticalJudgeGroupEntity::getGroupNo));
    return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
  }

  @Transactional
  public CompetitionPracticalJudgeGroupEntity createGroup(Long tenantId, Long userId, Long sessionId,
      CompetitionPracticalJudgeGroupReq req, Long planId) {
    CompetitionPracticalSessionEntity session = session(tenantId, sessionId);
    if (!java.util.Objects.equals(session.getPlanId(), planId)) {
      throw new BusinessException(ErrorCode.CONFLICT, "裁判组不属于当前执行方案");
    }
    lifecycleGuard.requireStageAllocationWritable(lifecycleGuard.lockStage(tenantId, session.getStageId()));
    lifecycleGuard.requirePublishedPlan(tenantId, planId, session.getEventId(), session.getStageId(),
        session.getEventWorkTypeId(), session.getEventWorkTypeLevelId());
    long officialCount = req.members().stream().filter(m -> !"standby".equalsIgnoreCase(m.memberRole())).count();
    if (officialCount != 3) throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "每个裁判组必须配置3名正式裁判");
    CompetitionPracticalJudgeGroupEntity existing = groupMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalJudgeGroupEntity>()
        .eq(CompetitionPracticalJudgeGroupEntity::getTenantId, tenantId)
        .eq(CompetitionPracticalJudgeGroupEntity::getSessionId, sessionId)
        .eq(CompetitionPracticalJudgeGroupEntity::getGroupNo, req.groupNo())
        .eq(CompetitionPracticalJudgeGroupEntity::getDeleted, 0));
    if (existing != null) return existing;
    Set<Long> ids = new HashSet<>();
    CompetitionPracticalJudgeGroupEntity group = new CompetitionPracticalJudgeGroupEntity();
    group.setId(IdWorker.getId()); group.setTenantId(tenantId); group.setSessionId(sessionId); group.setPlanId(planId);
    group.setGroupNo(req.groupNo()); group.setGroupName(req.groupName().trim()); group.setOfficialCount(3); group.setStatus("draft");
    List<Long> leaderIds = req.members().stream()
        .filter(member -> "leader".equalsIgnoreCase(member.memberRole()))
        .map(CompetitionPracticalJudgeMemberReq::refereeMemberId)
        .distinct()
        .toList();
    if (leaderIds.size() > 1) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "每个裁判组只能指定1名组长");
    }
    group.setLeaderRefereeMemberId(leaderIds.isEmpty() ? null : leaderIds.getFirst());
    group.setCreatedBy(userId); group.setUpdatedBy(userId);
    if (groupMapper.insert(group) != 1) throw new BusinessException(ErrorCode.CONFLICT, "裁判组保存失败");
    for (CompetitionPracticalJudgeMemberReq request : req.members()) {
      if (!ids.add(request.refereeMemberId())) throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "裁判组成员不能重复");
      CompetitionPracticalJudgeMemberEntity member = new CompetitionPracticalJudgeMemberEntity();
      member.setId(IdWorker.getId()); member.setTenantId(tenantId); member.setJudgeGroupId(group.getId()); member.setSessionId(sessionId);
      member.setRefereeMemberId(request.refereeMemberId()); member.setMemberRole("standby".equalsIgnoreCase(request.memberRole()) ? "standby" : "official");
      member.setStatus("active"); member.setCreatedBy(userId); member.setUpdatedBy(userId);
      if (memberMapper.insert(member) != 1) throw new BusinessException(ErrorCode.CONFLICT, "裁判组成员保存失败");
    }
    return group;
  }

  @Transactional(readOnly = true)
  public List<CompetitionPracticalJudgeMemberEntity> groupMembers(Long tenantId, Long groupId) {
    List<CompetitionPracticalJudgeMemberEntity> rows = memberMapper.selectList(new LambdaQueryWrapper<CompetitionPracticalJudgeMemberEntity>()
        .eq(CompetitionPracticalJudgeMemberEntity::getTenantId, tenantId)
        .eq(CompetitionPracticalJudgeMemberEntity::getJudgeGroupId, groupId)
        .eq(CompetitionPracticalJudgeMemberEntity::getDeleted, 0)
        .orderByAsc(CompetitionPracticalJudgeMemberEntity::getMemberRole)
        .orderByAsc(CompetitionPracticalJudgeMemberEntity::getId));
    return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
  }

  @Transactional
  public CompetitionPracticalJudgeMemberEntity activateStandby(Long tenantId, Long userId, Long memberId,
      Long replacedMemberId, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "备用裁判转正必须填写原因");
    }
    CompetitionPracticalJudgeMemberEntity standby = memberMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalJudgeMemberEntity>()
        .eq(CompetitionPracticalJudgeMemberEntity::getTenantId, tenantId)
        .eq(CompetitionPracticalJudgeMemberEntity::getId, memberId)
        .eq(CompetitionPracticalJudgeMemberEntity::getDeleted, 0));
    CompetitionPracticalJudgeMemberEntity official = memberMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalJudgeMemberEntity>()
        .eq(CompetitionPracticalJudgeMemberEntity::getTenantId, tenantId)
        .eq(CompetitionPracticalJudgeMemberEntity::getId, replacedMemberId)
        .eq(CompetitionPracticalJudgeMemberEntity::getDeleted, 0));
    if (standby != null && official != null && "official".equals(standby.getMemberRole())
        && "active".equals(standby.getStatus()) && "replaced".equals(official.getStatus())
        && java.util.Objects.equals(standby.getJudgeGroupId(), official.getJudgeGroupId())) {
      return standby;
    }
    if (standby == null || official == null || !"standby".equals(standby.getMemberRole())
        || !"official".equals(official.getMemberRole())
        || !java.util.Objects.equals(standby.getJudgeGroupId(), official.getJudgeGroupId())
        || !"active".equals(standby.getStatus()) || !"active".equals(official.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "备用裁判或被替换正式裁判状态无效");
    }
    CompetitionPracticalSessionEntity session = session(tenantId, standby.getSessionId());
    lifecycleGuard.requireStageAllocationWritable(lifecycleGuard.lockStage(tenantId, session.getStageId()));
    official.setStatus("replaced"); official.setUpdatedBy(userId);
    standby.setMemberRole("official"); standby.setUpdatedBy(userId);
    if (memberMapper.updateById(official) != 1 || memberMapper.updateById(standby) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "备用裁判转正状态已变化，请刷新后重试");
    }
    CompetitionPracticalJudgeGroupEntity judgeGroup = group(tenantId, standby.getJudgeGroupId());
    if (java.util.Objects.equals(judgeGroup.getLeaderRefereeMemberId(), official.getRefereeMemberId())) {
      judgeGroup.setLeaderRefereeMemberId(standby.getRefereeMemberId());
      judgeGroup.setUpdatedBy(userId);
      if (groupMapper.updateById(judgeGroup) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "裁判组组长补位状态已变化，请刷新后重试");
      }
    }
    return standby;
  }

  @Transactional
  public List<CompetitionPracticalAssignmentEntity> assign(Long tenantId, Long userId, CompetitionPracticalAssignmentReq req,
      Long planId) {
    CompetitionPracticalJudgeGroupEntity group = group(tenantId, req.judgeGroupId());
    if (!group.getSessionId().equals(req.sessionId())) throw new BusinessException(ErrorCode.CONFLICT, "裁判组不属于该场次");
    CompetitionPracticalSessionEntity session = session(tenantId, req.sessionId());
    lifecycleGuard.requireStageAllocationWritable(lifecycleGuard.lockStage(tenantId, session.getStageId()));
    CompetitionStageExecutionPlanEntity plan = lifecycleGuard.requirePublishedPlan(tenantId, planId,
        session.getEventId(), session.getStageId(), session.getEventWorkTypeId(), session.getEventWorkTypeLevelId());
    lifecycleGuard.requireIncludedRosterItem(tenantId, plan, req.rosterItemId(), req.contestantMemberId());
    assertContestantSessionFree(tenantId, req.contestantMemberId(), session);
    List<CompetitionPracticalJudgeMemberEntity> members = groupMembers(tenantId, group.getId());
    List<CompetitionPracticalAssignmentEntity> result = new ArrayList<>();
    for (Long refereeId : req.refereeMemberIds()) {
      CompetitionPracticalJudgeMemberEntity member = members.stream().filter(m -> refereeId.equals(m.getRefereeMemberId()) && "active".equals(m.getStatus())).findFirst().orElse(null);
      if (member == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "裁判不属于当前裁判组或已失效");
      if (isAvoided(tenantId, group.getSessionId(), refereeId, req.contestantMemberId())) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "裁判与选手存在有效回避关系");
      }
      CompetitionPracticalAssignmentEntity existing = assignmentMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalAssignmentEntity>()
          .eq(CompetitionPracticalAssignmentEntity::getTenantId, tenantId)
          .eq(CompetitionPracticalAssignmentEntity::getSessionId, req.sessionId())
          .eq(CompetitionPracticalAssignmentEntity::getContestantMemberId, req.contestantMemberId())
          .eq(CompetitionPracticalAssignmentEntity::getRefereeMemberId, refereeId)
          .eq(CompetitionPracticalAssignmentEntity::getDeleted, 0));
      if (existing != null) {
        result.add(existing);
        continue;
      }
      CompetitionPracticalAssignmentEntity row = new CompetitionPracticalAssignmentEntity();
      row.setId(IdWorker.getId()); row.setTenantId(tenantId); row.setPlanId(planId); row.setSessionId(req.sessionId());
      row.setJudgeGroupId(req.judgeGroupId()); row.setRosterItemId(req.rosterItemId()); row.setContestantMemberId(req.contestantMemberId());
      row.setRefereeMemberId(refereeId); row.setWorkstationCode(req.workstationCode()); row.setStatus("assigned");
      row.setAssignedAt(LocalDateTime.now()); row.setCreatedBy(userId); row.setUpdatedBy(userId);
      if (assignmentMapper.insert(row) != 1) throw new BusinessException(ErrorCode.CONFLICT, "实操选手分配失败");
      result.add(row);
    }
    return result;
  }

  @Transactional
  public CompetitionPracticalScoreSheetEntity openSheet(Long tenantId, Long userId, Long assignmentId) {
    CompetitionPracticalAssignmentEntity assignment = assignment(tenantId, assignmentId);
    requireScoreWriteScope(tenantId, userId, assignment);
    CompetitionPracticalScoreSheetEntity existing = sheet(tenantId, assignmentId);
    if (existing != null) return existing;
    CompetitionPracticalSessionEntity session = session(tenantId, assignment.getSessionId());
    CompetitionPracticalRuleEntity rule = assignment.getAssessmentSheetId() == null
        ? publishedRule(tenantId, assignment.getPlanId()) : null;
    CompetitionPracticalScoreSheetEntity row = new CompetitionPracticalScoreSheetEntity();
    row.setId(IdWorker.getId()); row.setTenantId(tenantId); row.setAssignmentId(assignmentId); row.setPlanId(assignment.getPlanId());
    row.setRuleId(rule == null ? null : rule.getId());
    row.setAssessmentSheetId(assignment.getAssessmentSheetId());
    row.setEventId(session.getEventId()); row.setStageId(session.getStageId());
    row.setEventWorkTypeId(session.getEventWorkTypeId()); row.setEventWorkTypeLevelId(session.getEventWorkTypeLevelId());
    row.setContestantMemberId(assignment.getContestantMemberId()); row.setRefereeMemberId(assignment.getRefereeMemberId());
    row.setRevisionNo(1); row.setStatus("draft"); row.setOpenedAt(LocalDateTime.now()); row.setCreatedBy(userId); row.setUpdatedBy(userId);
    if (sheetMapper.insert(row) != 1) throw new BusinessException(ErrorCode.CONFLICT, "评分单打开失败");
    return row;
  }

  @Transactional
  public CompetitionPracticalScoreSheetEntity saveSheet(Long tenantId, Long userId, Long sheetId, CompetitionPracticalScoreSaveReq req) {
    return persistSheet(tenantId, userId, sheetId, req, ScorePersistMode.DRAFT);
  }

  @Transactional
  public CompetitionPracticalScoreSheetEntity submitQuestions(
      Long tenantId, Long userId, Long sheetId, CompetitionPracticalScoreSaveReq req) {
    return persistSheet(tenantId, userId, sheetId, req, ScorePersistMode.SUBMIT_QUESTION);
  }

  @Transactional
  public CompetitionPracticalScoreSheetEntity lockSheet(Long tenantId, Long userId, Long sheetId, CompetitionPracticalScoreSaveReq req) {
    return persistSheet(tenantId, userId, sheetId, req, ScorePersistMode.LOCK_ALL);
  }

  private CompetitionPracticalScoreSheetEntity persistSheet(Long tenantId, Long userId, Long sheetId,
      CompetitionPracticalScoreSaveReq req, ScorePersistMode mode) {
    CompetitionPracticalScoreSheetEntity sheet = requireSheet(tenantId, sheetId);
    CompetitionPracticalAssignmentEntity assignment = assignment(tenantId, sheet.getAssignmentId());
    requireScoreWriteScope(tenantId, userId, assignment);
    if (!java.util.Objects.equals(sheet.getPlanId(), assignment.getPlanId())
        || !java.util.Objects.equals(sheet.getContestantMemberId(), assignment.getContestantMemberId())
        || !java.util.Objects.equals(sheet.getRefereeMemberId(), assignment.getRefereeMemberId())) {
      throw new BusinessException(ErrorCode.CONFLICT, "评分单与当前分配范围不一致");
    }
    if (!"draft".equals(sheet.getStatus()) && !"unlocked_for_rescore".equals(sheet.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "已锁定评分单不可修改");
    }
    CompetitionPracticalSessionEntity session = session(tenantId, assignment.getSessionId());
    List<CompetitionPracticalRuleItemEntity> items = sheet.getRuleId() == null
        ? List.of() : ruleItems(tenantId, sheet.getRuleId());
    List<SnapshotScoreItem> paperItems = snapshotScoringItems(tenantId, session);
    Map<String, SnapshotScoreItem> paperItemsByKey = new LinkedHashMap<>();
    for (SnapshotScoreItem item : paperItems) {
      paperItemsByKey.put(item.key(), item);
    }
    boolean snapshotMode = session.getPaperSnapshotId() != null;
    Set<Long> expectedRuleItems = new HashSet<>();
    Set<String> expectedPaperItems = new HashSet<>();
    for (CompetitionPracticalRuleItemEntity item : items) {
      if ("enabled".equals(item.getStatus())) expectedRuleItems.add(item.getId());
    }
    if (snapshotMode && paperItemsByKey.isEmpty()) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操组卷快照没有可用评分项");
    }
    expectedPaperItems.addAll(paperItemsByKey.keySet());
    Set<Long> actualRuleItems = new HashSet<>();
    Set<String> actualPaperItems = new HashSet<>();
    Set<Long> affectedQuestionIds = new HashSet<>();
    List<BigDecimal> effectiveEntryScores = new ArrayList<>();
    for (CompetitionPracticalScoreEntryReq request : req.entries()) {
      if (snapshotMode) {
        if (request.paperQuestionId() == null || request.paperItemNo() == null) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "竞赛实操评分必须填写快照题目和评分项");
        }
        String key = snapshotItemKey(request.paperQuestionId(), request.paperItemNo());
        SnapshotScoreItem item = paperItemsByKey.get(key);
        if (item == null || !actualPaperItems.add(key)) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "评分明细与实操题快照不匹配或重复");
        }
        affectedQuestionIds.add(request.paperQuestionId());
        effectiveEntryScores.add(validateSnapshotEntry(request, item));
      } else {
        if (request.ruleItemId() == null || !actualRuleItems.add(request.ruleItemId())
            || !expectedRuleItems.contains(request.ruleItemId())) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "评分明细与当前规则不匹配");
        }
        CompetitionPracticalRuleItemEntity item = items.stream()
            .filter(i -> i.getId().equals(request.ruleItemId())).findFirst().orElseThrow();
        effectiveEntryScores.add(ruleDomain.validateEntry(request.score(), request.deduction(),
            item.getMaxScore(), item.getDeductionAllowed() == 1));
      }
    }
    if (snapshotMode && mode != ScorePersistMode.DRAFT) {
      for (Long questionId : affectedQuestionIds) {
        Set<String> expectedForQuestion = new HashSet<>();
        Set<String> actualForQuestion = new HashSet<>();
        for (SnapshotScoreItem item : paperItemsByKey.values()) {
          if (Objects.equals(item.questionId(), questionId)) {
            expectedForQuestion.add(item.key());
          }
        }
        for (String key : actualPaperItems) {
          SnapshotScoreItem item = paperItemsByKey.get(key);
          if (item != null && Objects.equals(item.questionId(), questionId)) {
            actualForQuestion.add(key);
          }
        }
        if (!expectedForQuestion.equals(actualForQuestion)) {
          throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "提交前必须完成本题全部评分项");
        }
      }
    }
    if (mode == ScorePersistMode.LOCK_ALL && (snapshotMode
        ? !expectedPaperItems.equals(actualPaperItems)
        : !expectedRuleItems.equals(actualRuleItems))) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "锁定前必须完成全部评分项");
    }
    List<CompetitionPracticalScoreEntryEntity> old = entries(tenantId, sheetId);
    boolean rescoring = "unlocked_for_rescore".equals(sheet.getStatus());
    String entryStatus = mode == ScorePersistMode.DRAFT
        ? CompetitionPracticalQuestionStatuses.SCORING
        : CompetitionPracticalQuestionStatuses.SUBMITTED;
    Map<String, CompetitionPracticalScoreEntryEntity> oldByKey = new LinkedHashMap<>();
    for (CompetitionPracticalScoreEntryEntity entry : old) {
      String key = scoreEntryKey(snapshotMode, entry.getRuleItemId(),
          entry.getPaperQuestionId(), entry.getPaperItemNo());
      if (key != null) {
        oldByKey.put(key, entry);
      }
    }
    Set<String> incomingKeys = new HashSet<>();
    for (int index = 0; index < req.entries().size(); index++) {
      CompetitionPracticalScoreEntryReq request = req.entries().get(index);
      String key = scoreEntryKey(snapshotMode, request.ruleItemId(),
          request.paperQuestionId(), request.paperItemNo());
      if (key == null || !incomingKeys.add(key)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "评分明细与实操题快照不匹配或重复");
      }
      CompetitionPracticalScoreEntryEntity existing = oldByKey.get(key);
      if (existing != null) {
        if ((mode == ScorePersistMode.DRAFT || mode == ScorePersistMode.SUBMIT_QUESTION)
            && CompetitionPracticalQuestionStatuses.submitted(existing)
            && !rescoring) {
          throw new BusinessException(ErrorCode.CONFLICT, "该题已提交，不能再修改评分");
        }
        existing.setScore(effectiveEntryScores.get(index).setScale(2, RoundingMode.HALF_UP));
        existing.setDeduction(request.deduction().setScale(2, RoundingMode.HALF_UP));
        existing.setRemark(request.remark());
        existing.setStatus(entryStatus);
        existing.setUpdatedBy(userId);
        if (entryMapper.updateById(existing) != 1) {
          throw new BusinessException(ErrorCode.CONFLICT, "评分明细保存失败");
        }
        continue;
      }
      CompetitionPracticalScoreEntryEntity entry = new CompetitionPracticalScoreEntryEntity();
      entry.setId(IdWorker.getId());
      entry.setTenantId(tenantId);
      entry.setScoreSheetId(sheetId);
      entry.setRuleItemId(request.ruleItemId());
      entry.setPaperQuestionId(request.paperQuestionId());
      entry.setPaperItemNo(request.paperItemNo());
      entry.setScore(effectiveEntryScores.get(index).setScale(2, RoundingMode.HALF_UP));
      entry.setDeduction(request.deduction().setScale(2, RoundingMode.HALF_UP));
      entry.setRemark(request.remark());
      entry.setStatus(entryStatus);
      entry.setCreatedBy(userId);
      entry.setUpdatedBy(userId);
      if (entryMapper.insert(entry) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "评分明细保存失败");
      }
    }
    for (CompetitionPracticalScoreEntryEntity entry : old) {
      boolean affected = !snapshotMode || (entry.getPaperQuestionId() != null
          && affectedQuestionIds.contains(entry.getPaperQuestionId()));
      if (!affected) {
        continue;
      }
      String key = scoreEntryKey(snapshotMode, entry.getRuleItemId(),
          entry.getPaperQuestionId(), entry.getPaperItemNo());
      if (key != null && incomingKeys.contains(key)) {
        continue;
      }
      if ((mode == ScorePersistMode.DRAFT || mode == ScorePersistMode.SUBMIT_QUESTION)
          && CompetitionPracticalQuestionStatuses.submitted(entry)
          && !rescoring) {
        throw new BusinessException(ErrorCode.CONFLICT, "该题已提交，不能再修改评分");
      }
      if (entryMapper.deleteById(entry.getId()) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "评分明细状态已变化，请刷新后重试");
      }
    }
    List<CompetitionPracticalScoreEntryEntity> remaining = entries(tenantId, sheetId);
    BigDecimal total = remaining.stream()
        .map(CompetitionPracticalScoreEntryEntity::getScore)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    sheet.setTotalScore(total.setScale(2, RoundingMode.HALF_UP));
    sheet.setUpdatedBy(userId);
    if (sheetMapper.updateById(sheet) != 1) throw new BusinessException(ErrorCode.CONFLICT, "评分单保存失败");
    if (shouldLockSheet(mode, snapshotMode, expectedPaperItems, expectedRuleItems, remaining)) {
      return lockCurrentSheet(tenantId, userId, sheet);
    }
    return sheet;
  }

  private boolean shouldLockSheet(
      ScorePersistMode mode,
      boolean snapshotMode,
      Set<String> expectedPaperItems,
      Set<Long> expectedRuleItems,
      List<CompetitionPracticalScoreEntryEntity> remaining) {
    if (mode == ScorePersistMode.LOCK_ALL) {
      return true;
    }
    if (mode != ScorePersistMode.SUBMIT_QUESTION) {
      return false;
    }
    if (snapshotMode) {
      Set<String> submittedKeys = remaining.stream()
          .filter(CompetitionPracticalQuestionStatuses::submitted)
          .filter(entry -> entry.getPaperQuestionId() != null && entry.getPaperItemNo() != null)
          .map(entry -> snapshotItemKey(entry.getPaperQuestionId(), entry.getPaperItemNo()))
          .collect(java.util.stream.Collectors.toCollection(HashSet::new));
      return !expectedPaperItems.isEmpty() && expectedPaperItems.equals(submittedKeys);
    }
    Set<Long> submittedItems = remaining.stream()
        .filter(CompetitionPracticalQuestionStatuses::submitted)
        .map(CompetitionPracticalScoreEntryEntity::getRuleItemId)
        .filter(Objects::nonNull)
        .collect(java.util.stream.Collectors.toCollection(HashSet::new));
    return !expectedRuleItems.isEmpty() && expectedRuleItems.equals(submittedItems);
  }

  private CompetitionPracticalScoreSheetEntity lockCurrentSheet(
      Long tenantId, Long userId, CompetitionPracticalScoreSheetEntity sheet) {
    if ("submitted_locked".equals(sheet.getStatus())) {
      return sheet;
    }
    LocalDateTime now = LocalDateTime.now();
    sheet.setStatus("submitted_locked");
    sheet.setSubmittedAt(now);
    sheet.setLockedAt(now);
    sheet.setUpdatedBy(userId);
    if (sheetMapper.updateById(sheet) != 1) throw new BusinessException(ErrorCode.CONFLICT, "评分单锁定失败");
    writePracticalScore(tenantId, userId, sheet);
    return sheet;
  }

  @Transactional
  public CompetitionPracticalScoreSheetEntity unlockSheet(Long tenantId, Long userId, Long sheetId, String reason) {
    CompetitionPracticalScoreSheetEntity sheet = requireSheet(tenantId, sheetId);
    requireScoreWriteScope(tenantId, userId, assignment(tenantId, sheet.getAssignmentId()));
    if (!"submitted_locked".equals(sheet.getStatus())) throw new BusinessException(ErrorCode.CONFLICT, "仅锁定评分单可以解锁");
    if (reason == null || reason.isBlank()) throw new BusinessException(ErrorCode.BAD_REQUEST, "解锁必须填写原因");
    sheet.setStatus("unlocked_for_rescore"); sheet.setUnlockReason(reason.trim()); sheet.setUpdatedBy(userId);
    if (sheetMapper.updateById(sheet) != 1) throw new BusinessException(ErrorCode.CONFLICT, "评分单解锁失败");
    for (CompetitionPracticalScoreEntryEntity entry : entries(tenantId, sheetId)) {
      entry.setStatus(CompetitionPracticalQuestionStatuses.SCORING);
      entry.setUpdatedBy(userId);
      if (entryMapper.updateById(entry) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "评分明细解锁失败");
      }
    }
    return sheet;
  }

  @Transactional(readOnly = true)
  public CompetitionPracticalScoreSheetEntity requireSheet(Long tenantId, Long sheetId) {
    CompetitionPracticalScoreSheetEntity row = sheetMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalScoreSheetEntity>()
        .eq(CompetitionPracticalScoreSheetEntity::getTenantId, tenantId).eq(CompetitionPracticalScoreSheetEntity::getId, sheetId)
        .eq(CompetitionPracticalScoreSheetEntity::getDeleted, 0));
    if (row == null) throw new BusinessException(ErrorCode.NOT_FOUND, "实操评分单不存在"); return row;
  }

  public CompetitionPracticalSessionEntity sessionForApplication(Long tenantId, Long id) {
    return session(tenantId, id);
  }

  public CompetitionPracticalAssignmentEntity assignmentForApplication(Long tenantId, Long id) {
    return assignment(tenantId, id);
  }

  public Long assignmentSessionId(Long tenantId, Long assignmentId) {
    return assignment(tenantId, assignmentId).getSessionId();
  }

  public List<CompetitionPracticalScoreEntryEntity> entries(Long tenantId, Long sheetId) {
    if (sheetId == null) return new ArrayList<>();
    return entriesBySheetIds(tenantId, List.of(sheetId)).getOrDefault(sheetId, new ArrayList<>());
  }

  public Map<Long, List<CompetitionPracticalScoreEntryEntity>> entriesBySheetIds(
      Long tenantId, Collection<Long> sheetIds) {
    Map<Long, List<CompetitionPracticalScoreEntryEntity>> result = new LinkedHashMap<>();
    if (tenantId == null || sheetIds == null || sheetIds.isEmpty()) return result;
    List<Long> ids = sheetIds.stream().filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) return result;
    List<CompetitionPracticalScoreEntryEntity> rows = entryMapper.selectList(
        new LambdaQueryWrapper<CompetitionPracticalScoreEntryEntity>()
            .eq(CompetitionPracticalScoreEntryEntity::getTenantId, tenantId)
            .in(CompetitionPracticalScoreEntryEntity::getScoreSheetId, ids)
            .eq(CompetitionPracticalScoreEntryEntity::getDeleted, 0));
    for (CompetitionPracticalScoreEntryEntity row : rows == null ? List.<CompetitionPracticalScoreEntryEntity>of() : rows) {
      if (row == null || row.getScoreSheetId() == null) continue;
      result.computeIfAbsent(row.getScoreSheetId(), ignored -> new ArrayList<>()).add(row);
    }
    return result;
  }

  private List<SnapshotScoreItem> snapshotScoringItems(
      Long tenantId, CompetitionPracticalSessionEntity session) {
    if (session.getPaperSnapshotId() == null) {
      return List.of();
    }
    if (paperQuestionMapper == null || objectMapper == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "竞赛实操快照评分适配器未配置");
    }
    List<CompetitionMakeExamPaperQuestionEntity> questions = paperQuestionMapper.selectByPaper(
        tenantId, session.getPaperSnapshotId());
    if (questions == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "竞赛实操快照题目查询失败");
    }
    List<SnapshotScoreItem> result = new ArrayList<>();
    for (CompetitionMakeExamPaperQuestionEntity question : questions) {
      if (!"practical".equalsIgnoreCase(question.getQuestionType())) {
        continue;
      }
      if (question.getScoringItemsJson() == null || question.getScoringItemsJson().isBlank()) {
        continue;
      }
      try {
        JsonNode root = objectMapper.readTree(question.getScoringItemsJson());
        if (root == null || !root.isArray()) {
          throw new IllegalArgumentException("评分项不是数组");
        }
        for (JsonNode item : root) {
          int itemNo = item.path("itemNo").asInt(0);
          if (itemNo <= 0) throw new IllegalArgumentException("评分项序号无效");
          BigDecimal maxScore = decimal(item, "maxScore");
          String scoringMethod = item.path("scoringMethod").asText("").trim().toLowerCase(java.util.Locale.ROOT);
          if (scoringMethod.isBlank()) scoringMethod = "deductive";
          BigDecimal maxDeduction = decimalOptional(item, "maxDeduction");
          BigDecimal deductionStep = decimalOptional(item, "deductionStep");
          if (maxScore.signum() <= 0) throw new IllegalArgumentException("评分项分值无效");
          if ("additive".equals(scoringMethod)) {
            if (maxDeduction != null || deductionStep != null) {
              throw new IllegalArgumentException("加分制评分项不能配置扣分规则");
            }
          } else {
            if (maxDeduction == null || deductionStep == null || maxDeduction.signum() < 0
                || maxDeduction.compareTo(maxScore) > 0 || deductionStep.signum() <= 0
                || maxDeduction.remainder(deductionStep).signum() != 0) {
              throw new IllegalArgumentException("扣分制评分项扣分规则无效");
            }
            scoringMethod = "deductive";
          }
          result.add(new SnapshotScoreItem(
              question.getId(), itemNo, maxScore, maxDeduction, deductionStep, scoringMethod));
        }
      } catch (JsonProcessingException | IllegalArgumentException ex) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "竞赛实操题快照评分项格式异常");
      }
    }
    return result;
  }

  private BigDecimal validateSnapshotEntry(
      CompetitionPracticalScoreEntryReq request, SnapshotScoreItem item) {
    if (request.score() == null || request.deduction() == null
        || request.score().signum() < 0 || request.deduction().signum() < 0
        || request.score().compareTo(item.maxScore()) > 0) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "评分项超出竞赛实操快照上限");
    }
    if ("additive".equals(item.scoringMethod())) {
      if (request.deduction().signum() != 0) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "加分制评分项的扣分值必须为0");
      }
      return request.score().setScale(2, RoundingMode.HALF_UP);
    }
    if (item.maxDeduction() == null || item.deductionStep() == null
        || request.deduction().compareTo(item.maxDeduction()) > 0
        || request.deduction().compareTo(item.maxScore()) > 0
        || request.deduction().remainder(item.deductionStep()).signum() != 0) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "扣分不符合竞赛实操快照扣分规则");
    }
    return item.maxScore().subtract(request.deduction()).setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal decimal(JsonNode node, String field) {
    String value = node.path(field).asText("").trim();
    if (value.isBlank()) throw new IllegalArgumentException(field + "不能为空");
    return new BigDecimal(value);
  }

  private BigDecimal decimalOptional(JsonNode node, String field) {
    String value = node.path(field).asText("").trim();
    if (value.isBlank() || "null".equalsIgnoreCase(value)) return null;
    return new BigDecimal(value);
  }

  private String snapshotItemKey(Long questionId, Integer itemNo) {
    return questionId + ":" + itemNo;
  }

  private String scoreEntryKey(
      boolean snapshotMode, Long ruleItemId, Long paperQuestionId, Integer paperItemNo) {
    if (snapshotMode) {
      return paperQuestionId == null || paperItemNo == null
          ? null : snapshotItemKey(paperQuestionId, paperItemNo);
    }
    return ruleItemId == null ? null : "rule:" + ruleItemId;
  }

  private record SnapshotScoreItem(
      Long questionId,
      Integer itemNo,
      BigDecimal maxScore,
      BigDecimal maxDeduction,
      BigDecimal deductionStep,
      String scoringMethod) {
    private String key() {
      return questionId + ":" + itemNo;
    }
  }

  @Transactional(readOnly = true)
  public List<CompetitionPracticalAssignmentEntity> assignments(Long tenantId, Long sessionId) {
    List<CompetitionPracticalAssignmentEntity> rows = assignmentMapper.selectList(new LambdaQueryWrapper<CompetitionPracticalAssignmentEntity>()
        .eq(CompetitionPracticalAssignmentEntity::getTenantId, tenantId)
        .eq(CompetitionPracticalAssignmentEntity::getSessionId, sessionId)
        .eq(CompetitionPracticalAssignmentEntity::getDeleted, 0)
        .orderByAsc(CompetitionPracticalAssignmentEntity::getContestantMemberId)
        .orderByAsc(CompetitionPracticalAssignmentEntity::getRefereeMemberId));
    return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
  }

  private void writePracticalScore(Long tenantId, Long userId, CompetitionPracticalScoreSheetEntity sheet) {
    CompetitionPracticalScoreEntity current = practicalScoreMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalScoreEntity>()
        .eq(CompetitionPracticalScoreEntity::getTenantId, tenantId).eq(CompetitionPracticalScoreEntity::getRefereeAssignmentId, sheet.getAssignmentId())
        .eq(CompetitionPracticalScoreEntity::getCurrentFlag, 1).eq(CompetitionPracticalScoreEntity::getDeleted, 0));
    int revision = current == null ? 1 : current.getRevisionNo() + 1;
    if (current != null) { current.setCurrentFlag(0); current.setUpdatedBy(userId); practicalScoreMapper.updateById(current); }
    CompetitionPracticalScoreEntity score = new CompetitionPracticalScoreEntity(); score.setId(IdWorker.getId()); score.setTenantId(tenantId);
    score.setEventId(sheet.getEventId()); score.setStageId(sheet.getStageId()); score.setEventWorkTypeId(sheet.getEventWorkTypeId());
    score.setEventWorkTypeLevelId(sheet.getEventWorkTypeLevelId()); score.setContestantMemberId(sheet.getContestantMemberId());
    score.setRefereeMemberId(sheet.getRefereeMemberId()); score.setRefereeAssignmentId(sheet.getAssignmentId());
    score.setCriteriaVersionReference(sheet.getAssessmentSheetId() == null
        ? "practical-rule:" + sheet.getRuleId()
        : "practical-assessment:" + sheet.getAssessmentSheetId());
    score.setRevisionNo(revision); score.setStatus(CompetitionPracticalScoreStatus.SUBMITTED_LOCKED.getCode()); score.setTotalScore(sheet.getTotalScore());
    score.setCurrentFlag(1); score.setSourceSystem("competition"); score.setSourceReference("score-sheet:" + sheet.getId());
    score.setSubmittedAt(sheet.getSubmittedAt()); score.setLockedAt(sheet.getLockedAt()); score.setCreatedBy(userId); score.setUpdatedBy(userId);
    if (practicalScoreMapper.insert(score) != 1) throw new BusinessException(ErrorCode.CONFLICT, "实操裁判成绩保存失败");
  }

  @Transactional
  public CompetitionScoreSourceEntity completePracticalScore(
      Long tenantId, Long userId, Long assignmentId) {
    CompetitionPracticalScoreSheetEntity sheet = sheet(tenantId, assignmentId);
    if (sheet == null || !"submitted_locked".equals(sheet.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "组长完成考试前必须先提交本人全部题目评分");
    }
    return refreshPracticalSource(tenantId, userId, sheet);
  }

  @Transactional(readOnly = true)
  public boolean isPracticalScoreCompleted(Long tenantId, Long assignmentId) {
    CompetitionPracticalScoreSheetEntity sheet = sheet(tenantId, assignmentId);
    if (sheet == null) {
      return false;
    }
    Long count = scoreSourceMapper.selectCount(
        new LambdaQueryWrapper<CompetitionScoreSourceEntity>()
            .eq(CompetitionScoreSourceEntity::getTenantId, tenantId)
            .eq(CompetitionScoreSourceEntity::getEventId, sheet.getEventId())
            .eq(CompetitionScoreSourceEntity::getStageId, sheet.getStageId())
            .eq(CompetitionScoreSourceEntity::getEventWorkTypeId, sheet.getEventWorkTypeId())
            .eq(sheet.getEventWorkTypeLevelId() != null,
                CompetitionScoreSourceEntity::getEventWorkTypeLevelId,
                sheet.getEventWorkTypeLevelId())
            .isNull(sheet.getEventWorkTypeLevelId() == null,
                CompetitionScoreSourceEntity::getEventWorkTypeLevelId)
            .eq(CompetitionScoreSourceEntity::getContestantMemberId,
                sheet.getContestantMemberId())
            .eq(CompetitionScoreSourceEntity::getSourceType,
                CompetitionScoreSourceType.PRACTICAL.getCode())
            .eq(CompetitionScoreSourceEntity::getConfirmationStatus, "confirmed")
            .eq(CompetitionScoreSourceEntity::getCurrentFlag, 1)
            .eq(CompetitionScoreSourceEntity::getDeleted, 0));
    return count != null && count > 0;
  }

  @Transactional(readOnly = true)
  public Set<Long> confirmedPracticalContestantIds(
      Long tenantId,
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId,
      Collection<Long> contestantMemberIds) {
    Set<Long> result = new HashSet<>();
    if (tenantId == null || eventId == null || stageId == null || eventWorkTypeId == null
        || contestantMemberIds == null || contestantMemberIds.isEmpty()) {
      return result;
    }
    List<Long> ids = contestantMemberIds.stream().filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) return result;
    List<CompetitionScoreSourceEntity> rows = scoreSourceMapper.selectList(
        new LambdaQueryWrapper<CompetitionScoreSourceEntity>()
            .eq(CompetitionScoreSourceEntity::getTenantId, tenantId)
            .eq(CompetitionScoreSourceEntity::getEventId, eventId)
            .eq(CompetitionScoreSourceEntity::getStageId, stageId)
            .eq(CompetitionScoreSourceEntity::getEventWorkTypeId, eventWorkTypeId)
            .eq(eventWorkTypeLevelId != null,
                CompetitionScoreSourceEntity::getEventWorkTypeLevelId,
                eventWorkTypeLevelId)
            .isNull(eventWorkTypeLevelId == null,
                CompetitionScoreSourceEntity::getEventWorkTypeLevelId)
            .in(CompetitionScoreSourceEntity::getContestantMemberId, ids)
            .eq(CompetitionScoreSourceEntity::getSourceType,
                CompetitionScoreSourceType.PRACTICAL.getCode())
            .eq(CompetitionScoreSourceEntity::getConfirmationStatus, "confirmed")
            .eq(CompetitionScoreSourceEntity::getCurrentFlag, 1)
            .eq(CompetitionScoreSourceEntity::getDeleted, 0));
    for (CompetitionScoreSourceEntity row : rows == null ? List.<CompetitionScoreSourceEntity>of() : rows) {
      if (row != null && row.getContestantMemberId() != null) {
        result.add(row.getContestantMemberId());
      }
    }
    return result;
  }

  @Transactional
  public Long freezePlanScoreRule(Long tenantId, Long userId, CompetitionStageExecutionPlanEntity plan) {
    if (plan == null) {
      return null;
    }
    if (plan.getScoreRuleId() != null) {
      return plan.getScoreRuleId();
    }
    List<CompetitionScoreRuleEntity> rules = publishedScoreRules(tenantId, plan, true);
    if (rules.size() != 1 && plan.getEventWorkTypeLevelId() != null) {
      rules = publishedScoreRules(tenantId, plan, false);
    }
    if (rules.size() != 1 || rules.get(0) == null || rules.get(0).getId() == null) {
      return null;
    }
    plan.setScoreRuleId(rules.get(0).getId());
    plan.setUpdatedBy(userId);
    if (planMapper.updateById(plan) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操执行方案冻结计分规则失败，请刷新后重试");
    }
    return plan.getScoreRuleId();
  }

  private List<CompetitionScoreRuleEntity> publishedScoreRules(
      Long tenantId, CompetitionStageExecutionPlanEntity plan, boolean matchLevel) {
    List<CompetitionScoreRuleEntity> rules = scoreRuleMapper.selectList(new LambdaQueryWrapper<CompetitionScoreRuleEntity>()
        .eq(CompetitionScoreRuleEntity::getTenantId, tenantId)
        .eq(CompetitionScoreRuleEntity::getEventId, plan.getEventId())
        .eq(CompetitionScoreRuleEntity::getStageId, plan.getStageId())
        .eq(CompetitionScoreRuleEntity::getEventWorkTypeId, plan.getEventWorkTypeId())
        .eq(matchLevel && plan.getEventWorkTypeLevelId() != null,
            CompetitionScoreRuleEntity::getEventWorkTypeLevelId, plan.getEventWorkTypeLevelId())
        .isNull(!matchLevel || plan.getEventWorkTypeLevelId() == null,
            CompetitionScoreRuleEntity::getEventWorkTypeLevelId)
        .eq(CompetitionScoreRuleEntity::getStatus, "published")
        .eq(CompetitionScoreRuleEntity::getDeleted, 0));
    return rules == null ? List.of() : new ArrayList<>(rules);
  }

  private CompetitionScoreSourceEntity refreshPracticalSource(
      Long tenantId, Long userId, CompetitionPracticalScoreSheetEntity sheet) {
    CompetitionStageExecutionPlanEntity plan = planMapper.selectOne(new LambdaQueryWrapper<CompetitionStageExecutionPlanEntity>()
        .eq(CompetitionStageExecutionPlanEntity::getTenantId, tenantId)
        .eq(CompetitionStageExecutionPlanEntity::getId, sheet.getPlanId())
        .eq(CompetitionStageExecutionPlanEntity::getStatus, "published")
        .eq(CompetitionStageExecutionPlanEntity::getDeleted, 0));
    boolean scopeMismatch = plan != null && (
        !Objects.equals(plan.getEventId(), sheet.getEventId())
            || !Objects.equals(plan.getStageId(), sheet.getStageId())
            || !Objects.equals(plan.getEventWorkTypeId(), sheet.getEventWorkTypeId())
            || !Objects.equals(plan.getEventWorkTypeLevelId(), sheet.getEventWorkTypeLevelId()));
    if (plan == null || scopeMismatch) {
      log.warn("Practical score cannot bind published plan, sheetId={}, planId={}, eventId={}, stageId={}, workTypeId={}, levelId={}",
          sheet.getId(), sheet.getPlanId(), sheet.getEventId(), sheet.getStageId(),
          sheet.getEventWorkTypeId(), sheet.getEventWorkTypeLevelId());
      throw new BusinessException(ErrorCode.CONFLICT, "实操成绩无法绑定当前执行方案");
    }
    List<CompetitionPracticalScoreEntity> scores = practicalScoreMapper.selectList(new LambdaQueryWrapper<CompetitionPracticalScoreEntity>()
        .eq(CompetitionPracticalScoreEntity::getTenantId, tenantId).eq(CompetitionPracticalScoreEntity::getEventId, sheet.getEventId())
        .eq(CompetitionPracticalScoreEntity::getStageId, sheet.getStageId()).eq(CompetitionPracticalScoreEntity::getEventWorkTypeId, sheet.getEventWorkTypeId())
        .eq(sheet.getEventWorkTypeLevelId() != null, CompetitionPracticalScoreEntity::getEventWorkTypeLevelId, sheet.getEventWorkTypeLevelId())
        .isNull(sheet.getEventWorkTypeLevelId() == null, CompetitionPracticalScoreEntity::getEventWorkTypeLevelId)
        .eq(CompetitionPracticalScoreEntity::getContestantMemberId, sheet.getContestantMemberId()).eq(CompetitionPracticalScoreEntity::getCurrentFlag, 1)
        .eq(CompetitionPracticalScoreEntity::getDeleted, 0));
    List<CompetitionPracticalAverageCalculator.RefereeScore> candidates = (scores == null ? List.<CompetitionPracticalScoreEntity>of() : scores).stream()
        .map(s -> new CompetitionPracticalAverageCalculator.RefereeScore(s.getRefereeMemberId(), s.getTotalScore(), CompetitionPracticalScoreStatus.SUBMITTED_LOCKED, true)).toList();
    BigDecimal scoreDifference = snapshotScoreDifference(tenantId, sheet.getPlanId(), sheet.getContestantMemberId());
    CompetitionPracticalAverageCalculator.PracticalAverageResult result = averageCalculator.calculate(candidates, scoreDifference);
    if (!result.confirmed()) {
      String message = result.status()
          == CompetitionPracticalAverageCalculator.PracticalAverageStatus.PENDING_SCORE_DIFFERENCE
          ? "三名裁判评分分差超过题目允许范围，无法完成考试"
          : "三名裁判全部提交后才能完成考试";
      throw new BusinessException(ErrorCode.CONFLICT, message);
    }
    CompetitionScoreSourceEntity existing = scoreSourceMapper.selectOne(new LambdaQueryWrapper<CompetitionScoreSourceEntity>()
        .eq(CompetitionScoreSourceEntity::getTenantId, tenantId).eq(CompetitionScoreSourceEntity::getEventId, sheet.getEventId())
        .eq(CompetitionScoreSourceEntity::getStageId, sheet.getStageId()).eq(CompetitionScoreSourceEntity::getEventWorkTypeId, sheet.getEventWorkTypeId())
        .eq(sheet.getEventWorkTypeLevelId() != null, CompetitionScoreSourceEntity::getEventWorkTypeLevelId, sheet.getEventWorkTypeLevelId())
        .isNull(sheet.getEventWorkTypeLevelId() == null, CompetitionScoreSourceEntity::getEventWorkTypeLevelId)
        .eq(CompetitionScoreSourceEntity::getContestantMemberId, sheet.getContestantMemberId()).eq(CompetitionScoreSourceEntity::getSourceType, CompetitionScoreSourceType.PRACTICAL.getCode())
        .eq(CompetitionScoreSourceEntity::getCurrentFlag, 1).eq(CompetitionScoreSourceEntity::getDeleted, 0));
    if (existing != null && "confirmed".equals(existing.getConfirmationStatus())
        && existing.getRawScore() != null
        && result.score().compareTo(existing.getRawScore()) == 0) {
      publishConfirmedPracticalSource(existing, userId);
      return existing;
    }
    int revision = existing == null || existing.getRevisionNo() == null
        ? 1 : existing.getRevisionNo() + 1;
    if (existing != null) { existing.setCurrentFlag(0); existing.setUpdatedBy(userId); scoreSourceMapper.updateById(existing); }
    CompetitionScoreSourceEntity source = new CompetitionScoreSourceEntity(); source.setId(IdWorker.getId()); source.setTenantId(tenantId);
    source.setEventId(sheet.getEventId()); source.setStageId(sheet.getStageId()); source.setEventWorkTypeId(sheet.getEventWorkTypeId());
    source.setEventWorkTypeLevelId(sheet.getEventWorkTypeLevelId()); source.setContestantMemberId(sheet.getContestantMemberId());
    source.setSourceType(CompetitionScoreSourceType.PRACTICAL.getCode()); source.setRawScore(result.score()); source.setConfirmationStatus("confirmed");
    source.setSourceSystem("competition"); source.setSourceReference("practical-average:leader-confirmed:score-sheet:" + sheet.getId());
    source.setScoreRuleId(plan.getScoreRuleId());
    source.setRevisionNo(revision); source.setCurrentFlag(1); source.setAchievedAt(LocalDateTime.now()); source.setConfirmedAt(LocalDateTime.now()); source.setConfirmedBy(userId);
    source.setCreatedBy(userId); source.setUpdatedBy(userId); if (scoreSourceMapper.insert(source) != 1) throw new BusinessException(ErrorCode.CONFLICT, "实操成绩来源保存失败");
    publishConfirmedPracticalSource(source, userId);
    return source;
  }

  private void publishConfirmedPracticalSource(CompetitionScoreSourceEntity source, Long userId) {
    if (source == null || !"confirmed".equals(source.getConfirmationStatus())) {
      return;
    }
    if (automaticScorePipelineService != null) {
      automaticScorePipelineService.onConfirmedSource(source, userId);
    }
    if (scorePipelineOutboxService != null) {
      scorePipelineOutboxService.enqueue(source, userId);
    }
  }

  private BigDecimal snapshotScoreDifference(Long tenantId, Long planId, Long contestantMemberId) {
    if (planId == null || paperQuestionMapper == null) return null;
    // The practical session carries the immutable paper snapshot; resolve it
    // through the assignment/session chain so the threshold cannot come from
    // the mutable question-bank row.
    CompetitionPracticalAssignmentEntity assignment = assignmentMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalAssignmentEntity>()
        .eq(CompetitionPracticalAssignmentEntity::getTenantId, tenantId)
        .eq(CompetitionPracticalAssignmentEntity::getPlanId, planId)
        .eq(CompetitionPracticalAssignmentEntity::getContestantMemberId, contestantMemberId)
        .eq(CompetitionPracticalAssignmentEntity::getDeleted, 0)
        .last("LIMIT 1"));
    if (assignment == null) return null;
    CompetitionPracticalSessionEntity session = session(tenantId, assignment.getSessionId());
    return paperSnapshotScoreDifference(tenantId, session.getPaperSnapshotId());
  }

  public BigDecimal paperSnapshotScoreDifference(Long tenantId, Long paperSnapshotId) {
    if (tenantId == null || paperSnapshotId == null || paperQuestionMapper == null) return null;
    List<CompetitionMakeExamPaperQuestionEntity> questions =
        paperQuestionMapper.selectByPaper(tenantId, paperSnapshotId);
    if (questions == null) return null;
    return questions.stream().filter(q -> q != null && "practical".equalsIgnoreCase(q.getQuestionType())
        && q.getScoreDifference() != null).map(CompetitionMakeExamPaperQuestionEntity::getScoreDifference).findFirst().orElse(null);
  }

  /**
   * Uses the same frozen paper threshold as final score confirmation, but can
   * evaluate the currently submitted sheets before all three referees finish.
   */
  @Transactional(readOnly = true)
  public PracticalScoreDifferencePreview previewPracticalScoreDifference(
      Long tenantId, Long planId, Long contestantMemberId, List<BigDecimal> submittedScores) {
    return previewPracticalScoreDifference(
        snapshotScoreDifference(tenantId, planId, contestantMemberId), submittedScores);
  }

  public PracticalScoreDifferencePreview previewPracticalScoreDifference(
      BigDecimal allowedDifference, List<BigDecimal> submittedScores) {
    List<BigDecimal> scores = submittedScores == null ? List.of() : submittedScores.stream()
        .filter(Objects::nonNull)
        .toList();
    BigDecimal actual = scores.size() < 2 ? null
        : scores.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO)
            .subtract(scores.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
    boolean exceeded = actual != null && allowedDifference != null && allowedDifference.signum() >= 0
        && actual.compareTo(allowedDifference) > 0;
    String status = scores.size() < 2 ? "waiting"
        : exceeded ? "exceeded"
        : allowedDifference == null || allowedDifference.signum() < 0 ? "unrestricted" : "normal";
    return new PracticalScoreDifferencePreview(
        allowedDifference, actual, scores.size(), status, exceeded);
  }

  public record PracticalScoreDifferencePreview(
      BigDecimal allowedDifference,
      BigDecimal actualDifference,
      int submittedRefereeCount,
      String status,
      boolean exceeded) { }

  private CompetitionPracticalSessionEntity session(Long tenantId, Long id) { CompetitionPracticalSessionEntity row = sessionMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalSessionEntity>().eq(CompetitionPracticalSessionEntity::getTenantId, tenantId).eq(CompetitionPracticalSessionEntity::getId, id).eq(CompetitionPracticalSessionEntity::getDeleted, 0)); if (row == null) throw new BusinessException(ErrorCode.NOT_FOUND, "实操场次不存在"); return row; }
  private CompetitionPracticalJudgeGroupEntity group(Long tenantId, Long id) { CompetitionPracticalJudgeGroupEntity row = groupMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalJudgeGroupEntity>().eq(CompetitionPracticalJudgeGroupEntity::getTenantId, tenantId).eq(CompetitionPracticalJudgeGroupEntity::getId, id).eq(CompetitionPracticalJudgeGroupEntity::getDeleted, 0)); if (row == null) throw new BusinessException(ErrorCode.NOT_FOUND, "裁判组不存在"); return row; }
  private CompetitionPracticalAssignmentEntity assignment(Long tenantId, Long id) { CompetitionPracticalAssignmentEntity row = assignmentMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalAssignmentEntity>().eq(CompetitionPracticalAssignmentEntity::getTenantId, tenantId).eq(CompetitionPracticalAssignmentEntity::getId, id).eq(CompetitionPracticalAssignmentEntity::getDeleted, 0)); if (row == null) throw new BusinessException(ErrorCode.NOT_FOUND, "实操分配不存在"); return row; }
  private CompetitionPracticalRuleEntity publishedRule(Long tenantId, Long planId) { CompetitionPracticalSessionEntity session = sessionMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalSessionEntity>().eq(CompetitionPracticalSessionEntity::getTenantId, tenantId).eq(CompetitionPracticalSessionEntity::getPlanId, planId).eq(CompetitionPracticalSessionEntity::getDeleted, 0).last("LIMIT 1")); if (session == null) throw new BusinessException(ErrorCode.NOT_FOUND, "实操场次不存在"); CompetitionPracticalRuleEntity row = ruleMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalRuleEntity>().eq(CompetitionPracticalRuleEntity::getTenantId, tenantId).eq(CompetitionPracticalRuleEntity::getStageId, session.getStageId()).eq(CompetitionPracticalRuleEntity::getEventWorkTypeId, session.getEventWorkTypeId()).eq(session.getEventWorkTypeLevelId() != null, CompetitionPracticalRuleEntity::getEventWorkTypeLevelId, session.getEventWorkTypeLevelId()).isNull(session.getEventWorkTypeLevelId() == null, CompetitionPracticalRuleEntity::getEventWorkTypeLevelId).eq(CompetitionPracticalRuleEntity::getStatus, "published").eq(CompetitionPracticalRuleEntity::getDeleted, 0)); if (row == null) throw new BusinessException(ErrorCode.CONFLICT, "当前范围没有已发布实操规则"); return row; }
  private List<CompetitionPracticalRuleItemEntity> ruleItems(Long tenantId, Long ruleId) { List<CompetitionPracticalRuleItemEntity> rows = ruleItemMapper.selectList(new LambdaQueryWrapper<CompetitionPracticalRuleItemEntity>().eq(CompetitionPracticalRuleItemEntity::getTenantId, tenantId).eq(CompetitionPracticalRuleItemEntity::getRuleId, ruleId).eq(CompetitionPracticalRuleItemEntity::getDeleted, 0).orderByAsc(CompetitionPracticalRuleItemEntity::getSortOrder)); return rows == null ? List.of() : rows; }
  private CompetitionPracticalScoreSheetEntity sheet(Long tenantId, Long assignmentId) { return sheetMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalScoreSheetEntity>().eq(CompetitionPracticalScoreSheetEntity::getTenantId, tenantId).eq(CompetitionPracticalScoreSheetEntity::getAssignmentId, assignmentId).eq(CompetitionPracticalScoreSheetEntity::getDeleted, 0).orderByDesc(CompetitionPracticalScoreSheetEntity::getRevisionNo).last("LIMIT 1")); }
  private boolean isAvoided(Long tenantId, Long sessionId, Long refereeId, Long contestantId) { CompetitionPracticalSessionEntity session = session(tenantId, sessionId); Long count = avoidanceMapper.selectCount(new LambdaQueryWrapper<CompetitionRefereeAvoidanceEntity>().eq(CompetitionRefereeAvoidanceEntity::getTenantId, tenantId).eq(CompetitionRefereeAvoidanceEntity::getEventId, session.getEventId()).eq(CompetitionRefereeAvoidanceEntity::getStageId, session.getStageId()).eq(CompetitionRefereeAvoidanceEntity::getEventWorkTypeId, session.getEventWorkTypeId()).eq(CompetitionRefereeAvoidanceEntity::getRefereeMemberId, refereeId).eq(CompetitionRefereeAvoidanceEntity::getContestantMemberId, contestantId).eq(CompetitionRefereeAvoidanceEntity::getStatus, "active").eq(CompetitionRefereeAvoidanceEntity::getDeleted, 0)); return count != null && count > 0; }

  public void ensureInteractiveStage(Long tenantId, Long userId, Long sessionId) {
    ensureInteractiveStage(tenantId, userId, session(tenantId, sessionId));
  }

  private void ensureInteractiveStage(
      Long tenantId, Long userId, CompetitionPracticalSessionEntity session) {
    if (stageService != null) {
      stageService.ensureInProgressForInteractiveWrite(tenantId, userId, session.getStageId());
      return;
    }
    lifecycleGuard.requireInteractiveWrite(tenantId, session.getStageId());
  }

  private void requireScoreWriteScope(
      Long tenantId, Long userId, CompetitionPracticalAssignmentEntity assignment) {
    CompetitionPracticalSessionEntity session = session(tenantId, assignment.getSessionId());
    CompetitionStageExecutionPlanEntity plan = lifecycleGuard.requirePublishedPlan(tenantId, assignment.getPlanId(),
        session.getEventId(), session.getStageId(), session.getEventWorkTypeId(), session.getEventWorkTypeLevelId());
    lifecycleGuard.requireIncludedRosterItem(tenantId, plan, assignment.getRosterItemId(), assignment.getContestantMemberId());
    ensureInteractiveStage(tenantId, userId, session);
    List<CompetitionPracticalJudgeMemberEntity> members = groupMembers(tenantId, assignment.getJudgeGroupId());
    boolean official = members.stream().anyMatch(member -> assignment.getRefereeMemberId().equals(member.getRefereeMemberId())
        && "official".equals(member.getMemberRole()) && "active".equals(member.getStatus()));
    if (!official) {
      throw new BusinessException(ErrorCode.CONFLICT, "备用裁判未完成正式启用，不能形成有效成绩");
    }
  }

  private void assertContestantSessionFree(
      Long tenantId, Long contestantMemberId, CompetitionPracticalSessionEntity session) {
    if (sessionConflictService == null || contestantMemberMapper == null
        || session.getStartAt() == null || session.getEndAt() == null) {
      return;
    }
    CompetitionEventMemberEntity contestant = contestantMemberMapper.selectById(contestantMemberId);
    if (contestant == null || contestant.getPersonId() == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "选手身份缺失，无法校验场次冲突");
    }
    sessionConflictService.assertNoOverlap(
        tenantId, contestant.getPersonId(), session.getStartAt(), session.getEndAt(),
        null, session.getId(), null);
  }
}
