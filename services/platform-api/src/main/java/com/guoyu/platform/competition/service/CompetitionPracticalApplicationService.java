package com.guoyu.platform.competition.service;

import com.guoyu.platform.competition.dto.CompetitionPracticalAssignmentReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalJudgeGroupReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalRuleCreateReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalScoreSaveReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalSessionCreateReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalJudgeMemberActivateReq;
import com.guoyu.platform.competition.entity.CompetitionPracticalAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalJudgeGroupEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalJudgeMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalRuleEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalRuleItemEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreEntryEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreSheetEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalSessionEntity;
import com.guoyu.platform.competition.vo.CompetitionPracticalAssignmentVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalJudgeGroupVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalJudgeMemberVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalRuleItemVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalRuleVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalScoreEntryVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalScoreSheetVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalSessionVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetitionPracticalApplicationService {
  private final CompetitionStageApplicationService stageService;
  private final CompetitionStageExecutionPlanService planService;
  private final CompetitionPracticalRuleService ruleService;
  private final CompetitionPracticalExecutionService executionService;

  public CompetitionPracticalApplicationService(
      CompetitionStageApplicationService stageService,
      CompetitionStageExecutionPlanService planService,
      CompetitionPracticalRuleService ruleService,
      CompetitionPracticalExecutionService executionService) {
    this.stageService = stageService;
    this.planService = planService;
    this.ruleService = ruleService;
    this.executionService = executionService;
  }

  @Transactional(readOnly = true)
  public List<CompetitionPracticalRuleVO> rules(Long stageId, Long workTypeId, Long levelId) {
    var stage = stageService.get(stageId);
    return ruleService.list(tenant(), stageId, workTypeId, levelId).stream().map(r -> ruleVO(r, ruleService.items(tenant(), r.getId()))).toList();
  }

  @Transactional
  public CompetitionPracticalRuleVO createRule(Long stageId, CompetitionPracticalRuleCreateReq req) {
    var stage = stageService.assertCanManage(stageId);
    CompetitionPracticalRuleEntity rule = ruleService.create(tenant(), userId(), stage.getEventId(), stageId, req);
    return ruleVO(rule, ruleService.items(tenant(), rule.getId()));
  }

  @Transactional
  public CompetitionPracticalRuleVO publishRule(Long ruleId) {
    CompetitionPracticalRuleEntity existing = ruleService.get(tenant(), ruleId);
    stageService.assertCanManage(existing.getStageId());
    CompetitionPracticalRuleEntity rule = ruleService.publish(tenant(), userId(), ruleId);
    return ruleVO(rule, ruleService.items(tenant(), rule.getId()));
  }

  @Transactional(readOnly = true)
  public List<CompetitionPracticalSessionVO> sessions(Long planId) {
    var plan = planService.get(tenant(), planId); stageService.get(plan.getStageId());
    return executionService.sessions(tenant(), planId).stream().map(this::sessionVO).toList();
  }

  @Transactional
  public CompetitionPracticalSessionVO createSession(Long pathPlanId, CompetitionPracticalSessionCreateReq req) {
    if (!java.util.Objects.equals(pathPlanId, req.planId())) throw new com.guoyu.platform.common.exception.BusinessException(com.guoyu.platform.common.exception.ErrorCode.BAD_REQUEST, "路径方案与请求方案不一致");
    var plan = planService.get(tenant(), pathPlanId); stageService.assertCanManage(plan.getStageId());
    return sessionVO(executionService.createSession(tenant(), userId(), plan.getEventId(), plan.getStageId(), req,
        plan.getEventWorkTypeId(), plan.getEventWorkTypeLevelId()));
  }

  @Transactional
  public CompetitionPracticalSessionVO publishSession(Long sessionId) {
    var session = sessionById(sessionId); stageService.assertCanManage(session.getStageId());
    return sessionVO(executionService.publishSession(tenant(), sessionId));
  }

  @Transactional(readOnly = true)
  public List<CompetitionPracticalJudgeGroupVO> groups(Long sessionId) {
    var session = sessionById(sessionId); stageService.get(session.getStageId());
    return executionService.groups(tenant(), sessionId).stream().map(g -> groupVO(g, executionService.groupMembers(tenant(), g.getId()))).toList();
  }

  @Transactional
  public CompetitionPracticalJudgeGroupVO createGroup(Long sessionId, CompetitionPracticalJudgeGroupReq req) {
    var session = sessionById(sessionId); stageService.assertCanManage(session.getStageId());
    CompetitionPracticalJudgeGroupEntity group = executionService.createGroup(tenant(), userId(), sessionId, req, session.getPlanId());
    return groupVO(group, executionService.groupMembers(tenant(), group.getId()));
  }

  @Transactional
  public CompetitionPracticalJudgeMemberVO activateStandby(Long memberId, CompetitionPracticalJudgeMemberActivateReq req) {
    var member = executionService.activateStandby(tenant(), userId(), memberId, req.replacedMemberId(), req.reason());
    return new CompetitionPracticalJudgeMemberVO(member.getId(), member.getRefereeMemberId(), member.getMemberRole(), member.getStatus());
  }

  @Transactional(readOnly = true)
  public List<CompetitionPracticalAssignmentVO> assignments(Long sessionId) {
    var session = sessionById(sessionId); stageService.get(session.getStageId());
    return executionService.assignments(tenant(), sessionId).stream().map(this::assignmentVO).toList();
  }

  @Transactional
  public List<CompetitionPracticalAssignmentVO> assign(CompetitionPracticalAssignmentReq req) {
    var session = sessionById(req.sessionId()); stageService.assertCanManage(session.getStageId());
    return executionService.assign(tenant(), userId(), req, session.getPlanId()).stream().map(this::assignmentVO).toList();
  }

  @Transactional
  public CompetitionPracticalScoreSheetVO openSheet(Long assignmentId) {
    var assignment = assignmentById(assignmentId); stageService.assertCanManage(sessionById(assignment.getSessionId()).getStageId());
    return sheetVO(executionService.openSheet(tenant(), userId(), assignmentId));
  }

  @Transactional
  public CompetitionPracticalScoreSheetVO saveSheet(Long sheetId, CompetitionPracticalScoreSaveReq req, boolean lock) {
    CompetitionPracticalScoreSheetEntity sheet = executionService.requireSheet(tenant(), sheetId);
    stageService.assertCanManage(sessionById(executionService.assignmentSessionId(tenant(), sheet.getAssignmentId())).getStageId());
    CompetitionPracticalScoreSheetEntity saved = lock
        ? executionService.lockSheet(tenant(), userId(), sheetId, req)
        : executionService.saveSheet(tenant(), userId(), sheetId, req);
    return sheetVO(saved);
  }

  @Transactional
  public CompetitionPracticalScoreSheetVO unlockSheet(Long sheetId, String reason) {
    CompetitionPracticalScoreSheetEntity sheet = executionService.requireSheet(tenant(), sheetId);
    stageService.assertCanManage(sessionById(executionService.assignmentSessionId(tenant(), sheet.getAssignmentId())).getStageId());
    return sheetVO(executionService.unlockSheet(tenant(), userId(), sheetId, reason));
  }

  private CompetitionPracticalSessionEntity sessionById(Long id) { return executionService.sessionForApplication(tenant(), id); }
  private CompetitionPracticalAssignmentEntity assignmentById(Long id) { return executionService.assignmentForApplication(tenant(), id); }
  private CompetitionPracticalRuleVO ruleVO(CompetitionPracticalRuleEntity r, List<CompetitionPracticalRuleItemEntity> items) {
    return new CompetitionPracticalRuleVO(r.getId(), r.getEventId(), r.getStageId(), r.getEventWorkTypeId(), r.getEventWorkTypeLevelId(), r.getRuleVersion(), r.getRuleName(), r.getTotalScore(), r.getStatus(), r.getPublishedAt(), items.stream().map(i -> new CompetitionPracticalRuleItemVO(i.getId(), i.getItemCode(), i.getItemName(), i.getSortOrder(), i.getMaxScore(), i.getDeductionAllowed(), i.getCriteriaPayload(), i.getStatus())).toList());
  }
  private CompetitionPracticalSessionVO sessionVO(CompetitionPracticalSessionEntity s) { return new CompetitionPracticalSessionVO(s.getId(), s.getPlanId(), s.getEventId(), s.getStageId(), s.getEventWorkTypeId(), s.getEventWorkTypeLevelId(), s.getSessionCode(), s.getSessionName(), s.getStartAt(), s.getEndAt(), s.getCapacity(), s.getResourceId(), s.getStatus()); }
  private CompetitionPracticalJudgeGroupVO groupVO(CompetitionPracticalJudgeGroupEntity g, List<CompetitionPracticalJudgeMemberEntity> members) { return new CompetitionPracticalJudgeGroupVO(g.getId(), g.getSessionId(), g.getPlanId(), g.getGroupNo(), g.getGroupName(), g.getOfficialCount(), g.getStatus(), members.stream().map(m -> new CompetitionPracticalJudgeMemberVO(m.getId(), m.getRefereeMemberId(), m.getMemberRole(), m.getStatus())).toList()); }
  private CompetitionPracticalAssignmentVO assignmentVO(CompetitionPracticalAssignmentEntity a) { return new CompetitionPracticalAssignmentVO(a.getId(), a.getPlanId(), a.getSessionId(), a.getJudgeGroupId(), a.getRosterItemId(), a.getContestantMemberId(), a.getRefereeMemberId(), a.getWorkstationCode(), a.getStatus(), a.getAssignedAt()); }
  private CompetitionPracticalScoreSheetVO sheetVO(CompetitionPracticalScoreSheetEntity s) { return new CompetitionPracticalScoreSheetVO(s.getId(), s.getAssignmentId(), s.getRuleId(), s.getContestantMemberId(), s.getRefereeMemberId(), s.getRevisionNo(), s.getStatus(), s.getTotalScore(), s.getOpenedAt(), s.getSubmittedAt(), s.getLockedAt(), executionService.entries(tenant(), s.getId()).stream().map(e -> new CompetitionPracticalScoreEntryVO(e.getId(), e.getRuleItemId(), e.getScore(), e.getDeduction(), e.getRemark())).toList()); }
  private Long tenant() { return current().getTenantId(); }
  private Long userId() { return current().getUserId(); }
  private PlatformUserPrincipal current() { return SecurityContext.currentUser(); }
}
