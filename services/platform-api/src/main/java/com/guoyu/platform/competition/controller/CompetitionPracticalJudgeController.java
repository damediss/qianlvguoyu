package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionPracticalScoreSaveReq;
import com.guoyu.platform.competition.service.CompetitionPracticalJudgeService;
import com.guoyu.platform.competition.vo.CompetitionPracticalJudgeTaskVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/competition/practical-judge")
@PreAuthorize("hasAuthority('competition:practical-judge:score')")
public class CompetitionPracticalJudgeController {
  private final CompetitionPracticalJudgeService service;

  public CompetitionPracticalJudgeController(CompetitionPracticalJudgeService service) {
    this.service = service;
  }

  @GetMapping("/tasks")
  public ApiResponse<List<CompetitionPracticalJudgeTaskVO>> tasks() {
    return ApiResponse.ok(service.tasks());
  }

  @PostMapping("/tasks/{assignmentId}/open")
  public ApiResponse<CompetitionPracticalJudgeTaskVO> open(@PathVariable Long assignmentId) {
    return ApiResponse.ok(service.open(assignmentId));
  }

  @PostMapping("/tasks/{assignmentId}/confirm-start")
  @AuditLog(module = "competition", action = "practical-judge:confirm-start",
      targetType = "competition_practical_assignment")
  public ApiResponse<CompetitionPracticalJudgeTaskVO> confirmStart(
      @PathVariable Long assignmentId) {
    return ApiResponse.ok(service.confirmStart(assignmentId));
  }

  @PostMapping("/tasks/{assignmentId}/start-session")
  @AuditLog(module = "competition", action = "practical-judge:leader-start",
      targetType = "competition_practical_assignment")
  public ApiResponse<CompetitionPracticalJudgeTaskVO> startSession(
      @PathVariable Long assignmentId) {
    return ApiResponse.ok(service.startSession(assignmentId));
  }

  @PutMapping("/tasks/{assignmentId}/save")
  public ApiResponse<CompetitionPracticalJudgeTaskVO> save(
      @PathVariable Long assignmentId,
      @Valid @RequestBody CompetitionPracticalScoreSaveReq req) {
    return ApiResponse.ok(service.save(assignmentId, req, false));
  }

  @PostMapping("/tasks/{assignmentId}/submit")
  @AuditLog(module = "competition", action = "practical-judge:submit",
      targetType = "competition_practical_score_sheet")
  public ApiResponse<CompetitionPracticalJudgeTaskVO> submit(
      @PathVariable Long assignmentId,
      @Valid @RequestBody CompetitionPracticalScoreSaveReq req) {
    return ApiResponse.ok(service.save(assignmentId, req, true));
  }
}
