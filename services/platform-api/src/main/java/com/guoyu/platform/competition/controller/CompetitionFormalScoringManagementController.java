package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageQuery;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionFormalRescoreReq;
import com.guoyu.platform.competition.dto.CompetitionScoringTaskRetryReq;
import com.guoyu.platform.competition.vo.CompetitionFormalResultRevisionVO;
import com.guoyu.platform.competition.service.CompetitionFormalScoringManagementService;
import com.guoyu.platform.competition.vo.CompetitionFormalScoringTaskVO;
import com.guoyu.platform.security.SecurityContext;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/competition/formal-scoring")
public class CompetitionFormalScoringManagementController {
  private final CompetitionFormalScoringManagementService service;

  public CompetitionFormalScoringManagementController(
      CompetitionFormalScoringManagementService service) { this.service = service; }

  @GetMapping("/tasks")
  @PreAuthorize("hasAuthority('competition:score:view')")
  public ApiResponse<PageResponse<CompetitionFormalScoringTaskVO>> tasks(
      @RequestParam(required = false) Long sessionId,
      @RequestParam(required = false) Long eventId,
      @RequestParam(required = false) Long stageId,
      @RequestParam(required = false) Long eventWorkTypeId,
      @RequestParam(required = false) Long eventWorkTypeLevelId,
      @RequestParam(required = false) String status,
      @Valid @ModelAttribute PageQuery pageQuery) {
    return ApiResponse.ok(service.tasks(sessionId, eventId, stageId, eventWorkTypeId,
        eventWorkTypeLevelId, status, pageQuery));
  }

  @PostMapping("/tasks/{taskId}/retry")
  @PreAuthorize("hasAuthority('competition:score-task:retry')")
  @AuditLog(module = "competition", action = "formal-score-task:retry",
      targetType = "competition_formal_scoring_task", includeResult = true)
  public ApiResponse<CompetitionFormalScoringTaskVO> retry(@PathVariable Long taskId,
      @Valid @RequestBody CompetitionScoringTaskRetryReq request) {
    return ApiResponse.ok(service.retry(taskId, SecurityContext.currentUser().getUserId(),
        request.reason()));
  }

  @GetMapping("/candidates/{candidateExamId}/result-history")
  @PreAuthorize("hasAuthority('competition:score:view')")
  public ApiResponse<List<CompetitionFormalResultRevisionVO>> history(
      @PathVariable Long candidateExamId) {
    return ApiResponse.ok(service.history(candidateExamId));
  }

  @PostMapping("/candidates/{candidateExamId}/rescore")
  @PreAuthorize("hasAuthority('competition:score-task:retry')")
  @AuditLog(module = "competition", action = "formal-score:rescore",
      targetType = "competition_formal_scoring_task", includeResult = true)
  public ApiResponse<CompetitionFormalScoringTaskVO> rescore(@PathVariable Long candidateExamId,
      @Valid @RequestBody CompetitionFormalRescoreReq request) {
    return ApiResponse.ok(service.rescore(candidateExamId,
        SecurityContext.currentUser().getUserId(), request.reason()));
  }
}
