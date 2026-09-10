package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionStagePaperAssignmentBatchReq;
import com.guoyu.platform.competition.dto.CompetitionStagePaperAssignmentChangeReq;
import com.guoyu.platform.competition.dto.CompetitionStagePaperAssignmentQueryReq;
import com.guoyu.platform.competition.service.CompetitionStagePaperAssignmentApplicationService;
import com.guoyu.platform.competition.vo.CompetitionStagePaperAssignmentPlayerVO;
import com.guoyu.platform.competition.vo.CompetitionStagePaperAssignmentSummaryVO;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/competition/stages/{stageId}/paper-assignments")
public class CompetitionStagePaperAssignmentController {

  private final CompetitionStagePaperAssignmentApplicationService service;

  public CompetitionStagePaperAssignmentController(
      CompetitionStagePaperAssignmentApplicationService service) {
    this.service = service;
  }

  @GetMapping("/summary")
  @PreAuthorize("hasAuthority('competition:stage-paper-assignment:view')")
  public ApiResponse<CompetitionStagePaperAssignmentSummaryVO> summary(
      @PathVariable Long stageId) {
    return ApiResponse.ok(service.summary(stageId));
  }

  @GetMapping("/players")
  @PreAuthorize("hasAuthority('competition:stage-paper-assignment:view')")
  public ApiResponse<PageResponse<CompetitionStagePaperAssignmentPlayerVO>> players(
      @PathVariable Long stageId,
      @Valid @ModelAttribute CompetitionStagePaperAssignmentQueryReq req) {
    return ApiResponse.ok(service.page(stageId, req));
  }

  @GetMapping("/visualization")
  @PreAuthorize("hasAuthority('competition:stage-paper-assignment:view')")
  public ApiResponse<List<CompetitionStagePaperAssignmentPlayerVO>> visualization(
      @PathVariable Long stageId) {
    return ApiResponse.ok(service.visualization(stageId));
  }

  @PostMapping("/auto-allocate")
  @PreAuthorize("hasAuthority('competition:stage-paper-assignment:manage')")
  @AuditLog(module = "competition", action = "stage-paper-assignment:auto-allocate",
      targetType = "competition_stage_paper_assignment")
  public ApiResponse<CompetitionStagePaperAssignmentSummaryVO> autoAllocate(
      @PathVariable Long stageId) {
    return ApiResponse.ok(service.autoAllocate(stageId));
  }

  @PutMapping("/assignments/{contestantProfileId}")
  @PreAuthorize("hasAuthority('competition:stage-paper-assignment:manage')")
  @AuditLog(module = "competition", action = "stage-paper-assignment:assign",
      targetType = "competition_stage_paper_assignment")
  public ApiResponse<CompetitionStagePaperAssignmentPlayerVO> assign(
      @PathVariable Long stageId,
      @PathVariable Long contestantProfileId,
      @RequestBody(required = false) CompetitionStagePaperAssignmentChangeReq req) {
    return ApiResponse.ok(service.assign(stageId, contestantProfileId, req));
  }

  @PutMapping("/assignments/batch")
  @PreAuthorize("hasAuthority('competition:stage-paper-assignment:manage')")
  @AuditLog(module = "competition", action = "stage-paper-assignment:batch-assign",
      targetType = "competition_stage_paper_assignment")
  public ApiResponse<CompetitionStagePaperAssignmentSummaryVO> batchAssign(
      @PathVariable Long stageId,
      @Valid @RequestBody CompetitionStagePaperAssignmentBatchReq req) {
    return ApiResponse.ok(service.batchAssign(stageId, req));
  }

  @PutMapping
  @PreAuthorize("hasAuthority('competition:stage-paper-assignment:manage')")
  @AuditLog(module = "competition", action = "stage-paper-assignment:save",
      targetType = "competition_stage_paper_assignment")
  public ApiResponse<CompetitionStagePaperAssignmentSummaryVO> save(
      @PathVariable Long stageId,
      @Valid @RequestBody CompetitionStagePaperAssignmentBatchReq req) {
    return ApiResponse.ok(service.save(stageId, req));
  }

  @PostMapping("/confirm")
  @PreAuthorize("hasAuthority('competition:stage-paper-assignment:confirm') and hasAuthority('competition:stage-paper-assignment:manage')")
  @AuditLog(module = "competition", action = "stage-paper-assignment:confirm",
      targetType = "competition_stage_paper_assignment")
  public ApiResponse<CompetitionStagePaperAssignmentSummaryVO> confirm(
      @PathVariable Long stageId,
      @Valid @RequestBody CompetitionStagePaperAssignmentBatchReq req) {
    return ApiResponse.ok(service.confirm(stageId, req));
  }
}
