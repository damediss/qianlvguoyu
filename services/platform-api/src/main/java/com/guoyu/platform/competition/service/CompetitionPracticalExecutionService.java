package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.domain.CompetitionPracticalAverageCalculator;
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
import com.guoyu.platform.competition.entity.CompetitionRefereeAvoidanceEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.entity.CompetitionStageExecutionPlanEntity;
import com.guoyu.platform.competition.enums.CompetitionPracticalScoreStatus;
import com.guoyu.platform.competition.enums.CompetitionScoreSourceType;
import com.guoyu.platform.competition.mapper.CompetitionPracticalAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalJudgeGroupMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalJudgeMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalRuleItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalRuleMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalScoreEntryMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalScoreMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalScoreSheetMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeAvoidanceMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreRuleMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreSourceMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageExecutionPlanMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetitionPracticalExecutionService {
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
  private final CompetitionPracticalRuleDomainService ruleDomain = new CompetitionPracticalRuleDomainService();
  private final CompetitionPracticalAverageCalculator averageCalculator = new CompetitionPracticalAverageCalculator();

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
    lifecycleGuard.requireStageWritable(lifecycleGuard.lockStage(tenantId, stageId));
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
    requireScoreWriteScope(tenantId, assignment);
    CompetitionPracticalScoreSheetEntity existing = sheet(tenantId, assignmentId);
    if (existing != null) return existing;
    CompetitionPracticalRuleEntity rule = publishedRule(tenantId, assignment.getPlanId());
    CompetitionPracticalSessionEntity session = session(tenantId, assignment.getSessionId());
    CompetitionPracticalScoreSheetEntity row = new CompetitionPracticalScoreSheetEntity();
    row.setId(IdWorker.getId()); row.setTenantId(tenantId); row.setAssignmentId(assignmentId); row.setPlanId(assignment.getPlanId());
    row.setRuleId(rule.getId()); row.setEventId(session.getEventId()); row.setStageId(session.getStageId());
    row.setEventWorkTypeId(session.getEventWorkTypeId()); row.setEventWorkTypeLevelId(session.getEventWorkTypeLevelId());
    row.setContestantMemberId(assignment.getContestantMemberId()); row.setRefereeMemberId(assignment.getRefereeMemberId());
    row.setRevisionNo(1); row.setStatus("draft"); row.setOpenedAt(LocalDateTime.now()); row.setCreatedBy(userId); row.setUpdatedBy(userId);
    if (sheetMapper.insert(row) != 1) throw new BusinessException(ErrorCode.CONFLICT, "评分单打开失败");
    return row;
  }

  @Transactional
  public CompetitionPracticalScoreSheetEntity saveSheet(Long tenantId, Long userId, Long sheetId, CompetitionPracticalScoreSaveReq req) {
    return saveSheet(tenantId, userId, sheetId, req, false);
  }

  private CompetitionPracticalScoreSheetEntity saveSheet(Long tenantId, Long userId, Long sheetId,
      CompetitionPracticalScoreSaveReq req, boolean requireComplete) {
    CompetitionPracticalScoreSheetEntity sheet = requireSheet(tenantId, sheetId);
    CompetitionPracticalAssignmentEntity assignment = assignment(tenantId, sheet.getAssignmentId());
    requireScoreWriteScope(tenantId, assignment);
    if (!java.util.Objects.equals(sheet.getPlanId(), assignment.getPlanId())
        || !java.util.Objects.equals(sheet.getContestantMemberId(), assignment.getContestantMemberId())
        || !java.util.Objects.equals(sheet.getRefereeMemberId(), assignment.getRefereeMemberId())) {
      throw new BusinessException(ErrorCode.CONFLICT, "评分单与当前分配范围不一致");
    }
    if (!"draft".equals(sheet.getStatus()) && !"unlocked_for_rescore".equals(sheet.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "已锁定评分单不可修改");
    }
    List<CompetitionPracticalRuleItemEntity> items = ruleItems(tenantId, sheet.getRuleId());
    Set<Long> expected = new HashSet<>();
    for (CompetitionPracticalRuleItemEntity item : items) if ("enabled".equals(item.getStatus())) expected.add(item.getId());
    Set<Long> actual = new HashSet<>();
    BigDecimal total = BigDecimal.ZERO;
    for (CompetitionPracticalScoreEntryReq request : req.entries()) {
      if (!actual.add(request.ruleItemId()) || !expected.contains(request.ruleItemId())) throw new BusinessException(ErrorCode.BAD_REQUEST, "评分明细与当前规则不匹配");
      CompetitionPracticalRuleItemEntity item = items.stream().filter(i -> i.getId().equals(request.ruleItemId())).findFirst().orElseThrow();
      total = total.add(ruleDomain.validateEntry(request.score(), request.deduction(), item.getMaxScore(), item.getDeductionAllowed() == 1));
    }
    if (requireComplete && !expected.equals(actual)) throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "锁定前必须完成全部评分项");
    List<CompetitionPracticalScoreEntryEntity> old = entries(tenantId, sheetId);
    for (CompetitionPracticalScoreEntryEntity entry : old) entryMapper.deleteById(entry.getId());
    for (CompetitionPracticalScoreEntryReq request : req.entries()) {
      CompetitionPracticalScoreEntryEntity entry = new CompetitionPracticalScoreEntryEntity();
      entry.setId(IdWorker.getId()); entry.setTenantId(tenantId); entry.setScoreSheetId(sheetId); entry.setRuleItemId(request.ruleItemId());
      entry.setScore(request.score().setScale(2, RoundingMode.HALF_UP)); entry.setDeduction(request.deduction().setScale(2, RoundingMode.HALF_UP));
      entry.setRemark(request.remark()); entry.setCreatedBy(userId); entry.setUpdatedBy(userId);
      if (entryMapper.insert(entry) != 1) throw new BusinessException(ErrorCode.CONFLICT, "评分明细保存失败");
    }
    sheet.setTotalScore(total.setScale(2, RoundingMode.HALF_UP)); sheet.setUpdatedBy(userId);
    if (sheetMapper.updateById(sheet) != 1) throw new BusinessException(ErrorCode.CONFLICT, "评分单保存失败");
    return sheet;
  }

  @Transactional
  public CompetitionPracticalScoreSheetEntity lockSheet(Long tenantId, Long userId, Long sheetId, CompetitionPracticalScoreSaveReq req) {
    CompetitionPracticalScoreSheetEntity sheet = saveSheet(tenantId, userId, sheetId, req, true);
    LocalDateTime now = LocalDateTime.now(); sheet.setStatus("submitted_locked"); sheet.setSubmittedAt(now); sheet.setLockedAt(now); sheet.setUpdatedBy(userId);
    if (sheetMapper.updateById(sheet) != 1) throw new BusinessException(ErrorCode.CONFLICT, "评分单锁定失败");
    writePracticalScore(tenantId, userId, sheet);
    return sheet;
  }

  @Transactional
  public CompetitionPracticalScoreSheetEntity unlockSheet(Long tenantId, Long userId, Long sheetId, String reason) {
    CompetitionPracticalScoreSheetEntity sheet = requireSheet(tenantId, sheetId);
    requireScoreWriteScope(tenantId, assignment(tenantId, sheet.getAssignmentId()));
    if (!"submitted_locked".equals(sheet.getStatus())) throw new BusinessException(ErrorCode.CONFLICT, "仅锁定评分单可以解锁");
    if (reason == null || reason.isBlank()) throw new BusinessException(ErrorCode.BAD_REQUEST, "解锁必须填写原因");
    sheet.setStatus("unlocked_for_rescore"); sheet.setUnlockReason(reason.trim()); sheet.setUpdatedBy(userId);
    if (sheetMapper.updateById(sheet) != 1) throw new BusinessException(ErrorCode.CONFLICT, "评分单解锁失败");
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
    List<CompetitionPracticalScoreEntryEntity> rows = entryMapper.selectList(new LambdaQueryWrapper<CompetitionPracticalScoreEntryEntity>()
        .eq(CompetitionPracticalScoreEntryEntity::getTenantId, tenantId).eq(CompetitionPracticalScoreEntryEntity::getScoreSheetId, sheetId)
        .eq(CompetitionPracticalScoreEntryEntity::getDeleted, 0));
    return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
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
    score.setRefereeMemberId(sheet.getRefereeMemberId()); score.setRefereeAssignmentId(sheet.getAssignmentId()); score.setCriteriaVersionReference("practical-rule:" + sheet.getRuleId());
    score.setRevisionNo(revision); score.setStatus(CompetitionPracticalScoreStatus.SUBMITTED_LOCKED.getCode()); score.setTotalScore(sheet.getTotalScore());
    score.setCurrentFlag(1); score.setSourceSystem("competition"); score.setSourceReference("score-sheet:" + sheet.getId());
    score.setSubmittedAt(sheet.getSubmittedAt()); score.setLockedAt(sheet.getLockedAt()); score.setCreatedBy(userId); score.setUpdatedBy(userId);
    if (practicalScoreMapper.insert(score) != 1) throw new BusinessException(ErrorCode.CONFLICT, "实操裁判成绩保存失败");
    refreshPracticalSource(tenantId, userId, sheet);
  }

  private void refreshPracticalSource(Long tenantId, Long userId, CompetitionPracticalScoreSheetEntity sheet) {
    CompetitionStageExecutionPlanEntity plan = planMapper.selectOne(new LambdaQueryWrapper<CompetitionStageExecutionPlanEntity>()
        .eq(CompetitionStageExecutionPlanEntity::getTenantId, tenantId)
        .eq(CompetitionStageExecutionPlanEntity::getId, sheet.getPlanId())
        .eq(CompetitionStageExecutionPlanEntity::getStatus, "published")
        .eq(CompetitionStageExecutionPlanEntity::getDeleted, 0));
    if (plan == null || plan.getScoreRuleId() == null
        || !java.util.Objects.equals(plan.getEventId(), sheet.getEventId())
        || !java.util.Objects.equals(plan.getStageId(), sheet.getStageId())
        || !java.util.Objects.equals(plan.getEventWorkTypeId(), sheet.getEventWorkTypeId())
        || !java.util.Objects.equals(plan.getEventWorkTypeLevelId(), sheet.getEventWorkTypeLevelId())) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操成绩无法绑定当前执行方案的冻结计分规则");
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
    CompetitionPracticalAverageCalculator.PracticalAverageResult result = averageCalculator.calculate(candidates);
    if (!result.confirmed()) return;
    CompetitionScoreRuleEntity scoreRule = scoreRuleMapper.selectOne(new LambdaQueryWrapper<CompetitionScoreRuleEntity>()
        .eq(CompetitionScoreRuleEntity::getTenantId, tenantId).eq(CompetitionScoreRuleEntity::getStageId, sheet.getStageId())
        .eq(CompetitionScoreRuleEntity::getEventWorkTypeId, sheet.getEventWorkTypeId())
        .eq(CompetitionScoreRuleEntity::getId, plan.getScoreRuleId())
        .eq(sheet.getEventWorkTypeLevelId() != null, CompetitionScoreRuleEntity::getEventWorkTypeLevelId, sheet.getEventWorkTypeLevelId())
        .isNull(sheet.getEventWorkTypeLevelId() == null, CompetitionScoreRuleEntity::getEventWorkTypeLevelId)
        .eq(CompetitionScoreRuleEntity::getStatus, "published").eq(CompetitionScoreRuleEntity::getDeleted, 0));
    if (scoreRule == null) return;
    CompetitionScoreSourceEntity existing = scoreSourceMapper.selectOne(new LambdaQueryWrapper<CompetitionScoreSourceEntity>()
        .eq(CompetitionScoreSourceEntity::getTenantId, tenantId).eq(CompetitionScoreSourceEntity::getEventId, sheet.getEventId())
        .eq(CompetitionScoreSourceEntity::getStageId, sheet.getStageId()).eq(CompetitionScoreSourceEntity::getEventWorkTypeId, sheet.getEventWorkTypeId())
        .eq(sheet.getEventWorkTypeLevelId() != null, CompetitionScoreSourceEntity::getEventWorkTypeLevelId, sheet.getEventWorkTypeLevelId())
        .isNull(sheet.getEventWorkTypeLevelId() == null, CompetitionScoreSourceEntity::getEventWorkTypeLevelId)
        .eq(CompetitionScoreSourceEntity::getContestantMemberId, sheet.getContestantMemberId()).eq(CompetitionScoreSourceEntity::getSourceType, CompetitionScoreSourceType.PRACTICAL.getCode())
        .eq(CompetitionScoreSourceEntity::getCurrentFlag, 1).eq(CompetitionScoreSourceEntity::getDeleted, 0));
    int revision = existing == null ? 1 : existing.getRevisionNo() + 1;
    if (existing != null) { existing.setCurrentFlag(0); existing.setUpdatedBy(userId); scoreSourceMapper.updateById(existing); }
    CompetitionScoreSourceEntity source = new CompetitionScoreSourceEntity(); source.setId(IdWorker.getId()); source.setTenantId(tenantId);
    source.setEventId(sheet.getEventId()); source.setStageId(sheet.getStageId()); source.setEventWorkTypeId(sheet.getEventWorkTypeId());
    source.setEventWorkTypeLevelId(sheet.getEventWorkTypeLevelId()); source.setContestantMemberId(sheet.getContestantMemberId());
    source.setSourceType(CompetitionScoreSourceType.PRACTICAL.getCode()); source.setRawScore(result.score()); source.setConfirmationStatus("confirmed");
    source.setSourceSystem("competition"); source.setSourceReference("practical-average:score-sheet:" + sheet.getId()); source.setScoreRuleId(scoreRule.getId());
    source.setRevisionNo(revision); source.setCurrentFlag(1); source.setAchievedAt(LocalDateTime.now()); source.setConfirmedAt(LocalDateTime.now()); source.setConfirmedBy(userId);
    source.setCreatedBy(userId); source.setUpdatedBy(userId); if (scoreSourceMapper.insert(source) != 1) throw new BusinessException(ErrorCode.CONFLICT, "实操成绩来源保存失败");
  }

  private CompetitionPracticalSessionEntity session(Long tenantId, Long id) { CompetitionPracticalSessionEntity row = sessionMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalSessionEntity>().eq(CompetitionPracticalSessionEntity::getTenantId, tenantId).eq(CompetitionPracticalSessionEntity::getId, id).eq(CompetitionPracticalSessionEntity::getDeleted, 0)); if (row == null) throw new BusinessException(ErrorCode.NOT_FOUND, "实操场次不存在"); return row; }
  private CompetitionPracticalJudgeGroupEntity group(Long tenantId, Long id) { CompetitionPracticalJudgeGroupEntity row = groupMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalJudgeGroupEntity>().eq(CompetitionPracticalJudgeGroupEntity::getTenantId, tenantId).eq(CompetitionPracticalJudgeGroupEntity::getId, id).eq(CompetitionPracticalJudgeGroupEntity::getDeleted, 0)); if (row == null) throw new BusinessException(ErrorCode.NOT_FOUND, "裁判组不存在"); return row; }
  private CompetitionPracticalAssignmentEntity assignment(Long tenantId, Long id) { CompetitionPracticalAssignmentEntity row = assignmentMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalAssignmentEntity>().eq(CompetitionPracticalAssignmentEntity::getTenantId, tenantId).eq(CompetitionPracticalAssignmentEntity::getId, id).eq(CompetitionPracticalAssignmentEntity::getDeleted, 0)); if (row == null) throw new BusinessException(ErrorCode.NOT_FOUND, "实操分配不存在"); return row; }
  private CompetitionPracticalRuleEntity publishedRule(Long tenantId, Long planId) { CompetitionPracticalSessionEntity session = sessionMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalSessionEntity>().eq(CompetitionPracticalSessionEntity::getTenantId, tenantId).eq(CompetitionPracticalSessionEntity::getPlanId, planId).eq(CompetitionPracticalSessionEntity::getDeleted, 0).last("LIMIT 1")); if (session == null) throw new BusinessException(ErrorCode.NOT_FOUND, "实操场次不存在"); CompetitionPracticalRuleEntity row = ruleMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalRuleEntity>().eq(CompetitionPracticalRuleEntity::getTenantId, tenantId).eq(CompetitionPracticalRuleEntity::getStageId, session.getStageId()).eq(CompetitionPracticalRuleEntity::getEventWorkTypeId, session.getEventWorkTypeId()).eq(session.getEventWorkTypeLevelId() != null, CompetitionPracticalRuleEntity::getEventWorkTypeLevelId, session.getEventWorkTypeLevelId()).isNull(session.getEventWorkTypeLevelId() == null, CompetitionPracticalRuleEntity::getEventWorkTypeLevelId).eq(CompetitionPracticalRuleEntity::getStatus, "published").eq(CompetitionPracticalRuleEntity::getDeleted, 0)); if (row == null) throw new BusinessException(ErrorCode.CONFLICT, "当前范围没有已发布实操规则"); return row; }
  private List<CompetitionPracticalRuleItemEntity> ruleItems(Long tenantId, Long ruleId) { List<CompetitionPracticalRuleItemEntity> rows = ruleItemMapper.selectList(new LambdaQueryWrapper<CompetitionPracticalRuleItemEntity>().eq(CompetitionPracticalRuleItemEntity::getTenantId, tenantId).eq(CompetitionPracticalRuleItemEntity::getRuleId, ruleId).eq(CompetitionPracticalRuleItemEntity::getDeleted, 0).orderByAsc(CompetitionPracticalRuleItemEntity::getSortOrder)); return rows == null ? List.of() : rows; }
  private CompetitionPracticalScoreSheetEntity sheet(Long tenantId, Long assignmentId) { return sheetMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalScoreSheetEntity>().eq(CompetitionPracticalScoreSheetEntity::getTenantId, tenantId).eq(CompetitionPracticalScoreSheetEntity::getAssignmentId, assignmentId).eq(CompetitionPracticalScoreSheetEntity::getDeleted, 0).orderByDesc(CompetitionPracticalScoreSheetEntity::getRevisionNo).last("LIMIT 1")); }
  private boolean isAvoided(Long tenantId, Long sessionId, Long refereeId, Long contestantId) { CompetitionPracticalSessionEntity session = session(tenantId, sessionId); Long count = avoidanceMapper.selectCount(new LambdaQueryWrapper<CompetitionRefereeAvoidanceEntity>().eq(CompetitionRefereeAvoidanceEntity::getTenantId, tenantId).eq(CompetitionRefereeAvoidanceEntity::getEventId, session.getEventId()).eq(CompetitionRefereeAvoidanceEntity::getStageId, session.getStageId()).eq(CompetitionRefereeAvoidanceEntity::getEventWorkTypeId, session.getEventWorkTypeId()).eq(CompetitionRefereeAvoidanceEntity::getRefereeMemberId, refereeId).eq(CompetitionRefereeAvoidanceEntity::getContestantMemberId, contestantId).eq(CompetitionRefereeAvoidanceEntity::getStatus, "active").eq(CompetitionRefereeAvoidanceEntity::getDeleted, 0)); return count != null && count > 0; }

  private void requireScoreWriteScope(Long tenantId, CompetitionPracticalAssignmentEntity assignment) {
    CompetitionPracticalSessionEntity session = session(tenantId, assignment.getSessionId());
    CompetitionStageExecutionPlanEntity plan = lifecycleGuard.requirePublishedPlan(tenantId, assignment.getPlanId(),
        session.getEventId(), session.getStageId(), session.getEventWorkTypeId(), session.getEventWorkTypeLevelId());
    lifecycleGuard.requireIncludedRosterItem(tenantId, plan, assignment.getRosterItemId(), assignment.getContestantMemberId());
    lifecycleGuard.requireInteractiveWrite(tenantId, session.getStageId());
    List<CompetitionPracticalJudgeMemberEntity> members = groupMembers(tenantId, assignment.getJudgeGroupId());
    boolean official = members.stream().anyMatch(member -> assignment.getRefereeMemberId().equals(member.getRefereeMemberId())
        && "official".equals(member.getMemberRole()) && "active".equals(member.getStatus()));
    if (!official) {
      throw new BusinessException(ErrorCode.CONFLICT, "备用裁判未完成正式启用，不能形成有效成绩");
    }
  }
}
