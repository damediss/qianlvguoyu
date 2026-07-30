package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionScoreRuleCreateReq;
import com.guoyu.platform.competition.dto.CompetitionScoreRuleUpdateReq;
import com.guoyu.platform.competition.service.CompetitionScoreRuleApplicationService;
import com.guoyu.platform.competition.vo.CompetitionScoreRuleVO;
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
public class CompetitionScoreRuleController {

  private final CompetitionScoreRuleApplicationService service;

  public CompetitionScoreRuleController(CompetitionScoreRuleApplicationService service) {
    this.service = service;
  }

  @GetMapping("/events/{eventId}/score-rules")
  @PreAuthorize("hasAuthority('competition:score-rule:view')")
  public ApiResponse<List<CompetitionScoreRuleVO>> list(
      @PathVariable Long eventId,
      @RequestParam(required = false) Long stageId,
      @RequestParam(required = false) Long eventWorkTypeId) {
    return ApiResponse.ok(service.list(eventId, stageId, eventWorkTypeId));
  }

  @PostMapping("/events/{eventId}/score-rules")
  @PreAuthorize("hasAuthority('competition:score-rule:manage')")
  @AuditLog(module = "competition", action = "score-rule:create",
      targetType = "competition_score_rule")
  public ApiResponse<CompetitionScoreRuleVO> create(
      @PathVariable Long eventId,
      @Valid @RequestBody CompetitionScoreRuleCreateReq req) {
    return ApiResponse.ok(service.create(eventId, req));
  }

  @PutMapping("/score-rules/{ruleId}")
  @PreAuthorize("hasAuthority('competition:score-rule:manage')")
  @AuditLog(module = "competition", action = "score-rule:update",
      targetType = "competition_score_rule")
  public ApiResponse<CompetitionScoreRuleVO> update(
      @PathVariable Long ruleId,
      @Valid @RequestBody CompetitionScoreRuleUpdateReq req) {
    return ApiResponse.ok(service.update(ruleId, req));
  }

  @PostMapping("/score-rules/{ruleId}/publish")
  @PreAuthorize("hasAuthority('competition:score-rule:manage')")
  @AuditLog(module = "competition", action = "score-rule:publish",
      targetType = "competition_score_rule")
  public ApiResponse<CompetitionScoreRuleVO> publish(@PathVariable Long ruleId) {
    return ApiResponse.ok(service.publish(ruleId));
  }
}
