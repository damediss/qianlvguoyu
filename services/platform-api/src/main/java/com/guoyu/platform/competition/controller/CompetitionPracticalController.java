package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionPracticalAssignmentReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalJudgeGroupReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalJudgeMemberActivateReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalRuleCreateReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalScoreSaveReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalScoreUnlockReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalSessionCreateReq;
import com.guoyu.platform.competition.service.CompetitionPracticalApplicationService;
import com.guoyu.platform.competition.vo.CompetitionPracticalAssignmentVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalJudgeGroupVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalRuleVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalScoreSheetVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalSessionVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/competition")
public class CompetitionPracticalController {
  private final CompetitionPracticalApplicationService service;

  public CompetitionPracticalController(CompetitionPracticalApplicationService service) { this.service = service; }

  @GetMapping("/stages/{stageId}/practical-rules")
  @PreAuthorize("hasAuthority('competition:practical-rule:view')")
  public ApiResponse<List<CompetitionPracticalRuleVO>> rules(@PathVariable Long stageId, @RequestParam Long eventWorkTypeId, @RequestParam(required = false) Long eventWorkTypeLevelId) {
    return ApiResponse.ok(service.rules(stageId, eventWorkTypeId, eventWorkTypeLevelId));
  }

  @PostMapping("/stages/{stageId}/practical-rules")
  @PreAuthorize("hasAuthority('competition:practical-rule:manage')")
  @AuditLog(module = "competition", action = "practical-rule:create", targetType = "competition_practical_rule")
  public ApiResponse<CompetitionPracticalRuleVO> createRule(@PathVariable Long stageId, @Valid @RequestBody CompetitionPracticalRuleCreateReq req) { return ApiResponse.ok(service.createRule(stageId, req)); }

  @PostMapping("/practical-rules/{ruleId}/publish")
  @PreAuthorize("hasAuthority('competition:practical-rule:publish')")
  @AuditLog(module = "competition", action = "practical-rule:publish", targetType = "competition_practical_rule")
  public ApiResponse<CompetitionPracticalRuleVO> publishRule(@PathVariable Long ruleId) { return ApiResponse.ok(service.publishRule(ruleId)); }

  @GetMapping("/execution-plans/{planId}/practical-sessions")
  @PreAuthorize("hasAuthority('competition:practical-session:view')")
  public ApiResponse<List<CompetitionPracticalSessionVO>> sessions(@PathVariable Long planId) { return ApiResponse.ok(service.sessions(planId)); }

  @PostMapping("/execution-plans/{planId}/practical-sessions")
  @PreAuthorize("hasAuthority('competition:practical-session:manage')")
  @AuditLog(module = "competition", action = "practical-session:create", targetType = "competition_practical_session")
  public ApiResponse<CompetitionPracticalSessionVO> createSession(@PathVariable Long planId, @Valid @RequestBody CompetitionPracticalSessionCreateReq req) { return ApiResponse.ok(service.createSession(planId, req)); }

  @PostMapping("/practical-sessions/{sessionId}/publish")
  @PreAuthorize("hasAuthority('competition:practical-session:manage')")
  public ApiResponse<CompetitionPracticalSessionVO> publishSession(@PathVariable Long sessionId) { return ApiResponse.ok(service.publishSession(sessionId)); }

  @GetMapping("/practical-sessions/{sessionId}/judge-groups")
  @PreAuthorize("hasAuthority('competition:judge-group:view')")
  public ApiResponse<List<CompetitionPracticalJudgeGroupVO>> groups(@PathVariable Long sessionId) { return ApiResponse.ok(service.groups(sessionId)); }

  @GetMapping("/practical-sessions/{sessionId}/assignments")
  @PreAuthorize("hasAuthority('competition:practical-assignment:view')")
  public ApiResponse<List<CompetitionPracticalAssignmentVO>> assignments(@PathVariable Long sessionId) { return ApiResponse.ok(service.assignments(sessionId)); }

  @PostMapping("/practical-sessions/{sessionId}/judge-groups")
  @PreAuthorize("hasAuthority('competition:judge-group:manage')")
  @AuditLog(module = "competition", action = "judge-group:create", targetType = "competition_practical_judge_group")
  public ApiResponse<CompetitionPracticalJudgeGroupVO> createGroup(@PathVariable Long sessionId, @Valid @RequestBody CompetitionPracticalJudgeGroupReq req) { return ApiResponse.ok(service.createGroup(sessionId, req)); }

  @PostMapping("/practical-judge-members/{memberId}/activate")
  @PreAuthorize("hasAuthority('competition:judge-group:manage')")
  @AuditLog(module = "competition", action = "judge-member:activate-standby", targetType = "competition_practical_judge_member")
  public ApiResponse<com.guoyu.platform.competition.vo.CompetitionPracticalJudgeMemberVO> activateStandby(
      @PathVariable Long memberId, @Valid @RequestBody CompetitionPracticalJudgeMemberActivateReq req) {
    return ApiResponse.ok(service.activateStandby(memberId, req));
  }

  @PostMapping("/practical-assignments")
  @PreAuthorize("hasAuthority('competition:practical-assignment:manage')")
  @AuditLog(module = "competition", action = "practical-assignment:create", targetType = "competition_practical_assignment")
  public ApiResponse<List<CompetitionPracticalAssignmentVO>> assign(@Valid @RequestBody CompetitionPracticalAssignmentReq req) { return ApiResponse.ok(service.assign(req)); }

  @PostMapping("/practical-assignments/{assignmentId}/score-sheet/open")
  @PreAuthorize("hasAuthority('competition:practical-score:manage')")
  public ApiResponse<CompetitionPracticalScoreSheetVO> openSheet(@PathVariable Long assignmentId) { return ApiResponse.ok(service.openSheet(assignmentId)); }

  @PutMapping("/practical-score-sheets/{sheetId}/save")
  @PreAuthorize("hasAuthority('competition:practical-score:manage')")
  public ApiResponse<CompetitionPracticalScoreSheetVO> saveSheet(@PathVariable Long sheetId, @Valid @RequestBody CompetitionPracticalScoreSaveReq req) { return ApiResponse.ok(service.saveSheet(sheetId, req, false)); }

  @PostMapping("/practical-score-sheets/{sheetId}/lock")
  @PreAuthorize("hasAuthority('competition:practical-score:manage')")
  @AuditLog(module = "competition", action = "practical-score:lock", targetType = "competition_practical_score_sheet")
  public ApiResponse<CompetitionPracticalScoreSheetVO> lockSheet(@PathVariable Long sheetId, @Valid @RequestBody CompetitionPracticalScoreSaveReq req) { return ApiResponse.ok(service.saveSheet(sheetId, req, true)); }

  @PostMapping("/practical-score-sheets/{sheetId}/unlock")
  @PreAuthorize("hasAuthority('competition:practical-score:unlock')")
  @AuditLog(module = "competition", action = "practical-score:unlock", targetType = "competition_practical_score_sheet")
  public ApiResponse<CompetitionPracticalScoreSheetVO> unlockSheet(@PathVariable Long sheetId, @Valid @RequestBody CompetitionPracticalScoreUnlockReq req) { return ApiResponse.ok(service.unlockSheet(sheetId, req.reason())); }
}
