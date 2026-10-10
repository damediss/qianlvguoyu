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
import org.springframework.web.bind.annotation.RequestParam;
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

  public ApiResponse<CompetitionStagePaperAssignmentSummaryVO> summary(
      Long stageId, String contentType) {
    return ApiResponse.ok(service.summary(stageId, contentType));
  }

  @GetMapping("/players")
  @PreAuthorize("hasAuthority('competition:stage-paper-assignment:view')")
  public ApiResponse<PageResponse<CompetitionStagePaperAssignmentPlayerVO>> players(
      @PathVariable Long stageId,
      @Valid @ModelAttribute CompetitionStagePaperAssignmentQueryReq req) {
    return ApiResponse.ok(service.page(stageId, req));
  }

  public ApiResponse<PageResponse<CompetitionStagePaperAssignmentPlayerVO>> players(
      Long stageId, CompetitionStagePaperAssignmentQueryReq req, String contentType) {
    return ApiResponse.ok(service.page(stageId, req, contentType));
  }

  @GetMapping("/visualization")
  @PreAuthorize("hasAuthority('competition:stage-paper-assignment:view')")
  public ApiResponse<List<CompetitionStagePaperAssignmentPlayerVO>> visualization(
      @PathVariable Long stageId) {
    return ApiResponse.ok(service.visualization(stageId));
  }

  public ApiResponse<List<CompetitionStagePaperAssignmentPlayerVO>> visualization(
      Long stageId, String contentType) {
    return ApiResponse.ok(service.visualization(stageId, contentType));
  }

  @PostMapping("/auto-allocate")
  @PreAuthorize("hasAuthority('competition:stage-paper-assignment:manage')")
  @AuditLog(module = "competition", action = "stage-paper-assignment:auto-allocate",
      targetType = "competition_stage_paper_assignment")
  public ApiResponse<CompetitionStagePaperAssignmentSummaryVO> autoAllocate(
      @PathVariable Long stageId) {
    return ApiResponse.ok(service.autoAllocate(stageId));
  }

  public ApiResponse<CompetitionStagePaperAssignmentSummaryVO> autoAllocate(
      Long stageId, String contentType) {
    return ApiResponse.ok(service.autoAllocate(stageId, contentType));
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

  public ApiResponse<CompetitionStagePaperAssignmentPlayerVO> assign(
      Long stageId, Long contestantProfileId,
      CompetitionStagePaperAssignmentChangeReq req, String contentType) {
    return ApiResponse.ok(service.assign(stageId, contestantProfileId, req, contentType));
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

  public ApiResponse<CompetitionStagePaperAssignmentSummaryVO> batchAssign(
      Long stageId, CompetitionStagePaperAssignmentBatchReq req, String contentType) {
    return ApiResponse.ok(service.batchAssign(stageId, req, contentType));
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

  public ApiResponse<CompetitionStagePaperAssignmentSummaryVO> save(
      Long stageId, CompetitionStagePaperAssignmentBatchReq req, String contentType) {
    return ApiResponse.ok(service.save(stageId, req, contentType));
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

  @GetMapping(value = "/summary", params = "contentType")
  @PreAuthorize("hasAuthority('competition:stage-paper-assignment:view')")
  public ApiResponse<CompetitionStagePaperAssignmentSummaryVO> summaryByContentType(
      @PathVariable Long stageId,
      @RequestParam(defaultValue = "theory") String contentType) {
    return summary(stageId, contentType);
  }

  @GetMapping(value = "/players", params = "contentType")
  @PreAuthorize("hasAuthority('competition:stage-paper-assignment:view')")
  public ApiResponse<PageResponse<CompetitionStagePaperAssignmentPlayerVO>> playersByContentType(
      @PathVariable Long stageId,
      @Valid @ModelAttribute CompetitionStagePaperAssignmentQueryReq req,
      @RequestParam(defaultValue = "theory") String contentType) {
    return players(stageId, req, contentType);
  }

  @GetMapping(value = "/visualization", params = "contentType")
  @PreAuthorize("hasAuthority('competition:stage-paper-assignment:view')")
  public ApiResponse<List<CompetitionStagePaperAssignmentPlayerVO>> visualizationByContentType(
      @PathVariable Long stageId,
      @RequestParam(defaultValue = "theory") String contentType) {
    return visualization(stageId, contentType);
  }

  @PostMapping(value = "/auto-allocate", params = "contentType")
  @PreAuthorize("hasAuthority('competition:stage-paper-assignment:manage')")
  @AuditLog(module = "competition", action = "stage-paper-assignment:auto-allocate",
      targetType = "competition_stage_paper_assignment")
  public ApiResponse<CompetitionStagePaperAssignmentSummaryVO> autoAllocateByContentType(
      @PathVariable Long stageId,
      @RequestParam(defaultValue = "theory") String contentType) {
    return autoAllocate(stageId, contentType);
  }

  @PutMapping(value = "/assignments/{contestantProfileId}", params = "contentType")
  @PreAuthorize("hasAuthority('competition:stage-paper-assignment:manage')")
  @AuditLog(module = "competition", action = "stage-paper-assignment:assign",
      targetType = "competition_stage_paper_assignment")
  public ApiResponse<CompetitionStagePaperAssignmentPlayerVO> assignByContentType(
      @PathVariable Long stageId,
      @PathVariable Long contestantProfileId,
      @RequestBody(required = false) CompetitionStagePaperAssignmentChangeReq req,
      @RequestParam(defaultValue = "theory") String contentType) {
    return assign(stageId, contestantProfileId, req, contentType);
  }

  @PutMapping(value = "/assignments/batch", params = "contentType")
  @PreAuthorize("hasAuthority('competition:stage-paper-assignment:manage')")
  @AuditLog(module = "competition", action = "stage-paper-assignment:batch-assign",
      targetType = "competition_stage_paper_assignment")
  public ApiResponse<CompetitionStagePaperAssignmentSummaryVO> batchAssignByContentType(
      @PathVariable Long stageId,
      @Valid @RequestBody CompetitionStagePaperAssignmentBatchReq req,
      @RequestParam(defaultValue = "theory") String contentType) {
    return batchAssign(stageId, req, contentType);
  }

  @PutMapping(params = "contentType")
  @PreAuthorize("hasAuthority('competition:stage-paper-assignment:manage')")
  @AuditLog(module = "competition", action = "stage-paper-assignment:save",
      targetType = "competition_stage_paper_assignment")
  public ApiResponse<CompetitionStagePaperAssignmentSummaryVO> saveByContentType(
      @PathVariable Long stageId,
      @Valid @RequestBody CompetitionStagePaperAssignmentBatchReq req,
      @RequestParam(defaultValue = "theory") String contentType) {
    return save(stageId, req, contentType);
  }

  @PostMapping(value = "/confirm", params = "contentType")
  @PreAuthorize("hasAuthority('competition:stage-paper-assignment:confirm') and hasAuthority('competition:stage-paper-assignment:manage')")
  @AuditLog(module = "competition", action = "stage-paper-assignment:confirm",
      targetType = "competition_stage_paper_assignment")
  public ApiResponse<CompetitionStagePaperAssignmentSummaryVO> confirmByContentType(
      @PathVariable Long stageId,
      @Valid @RequestBody CompetitionStagePaperAssignmentBatchReq req,
      @RequestParam(defaultValue = "theory") String contentType) {
    return ApiResponse.ok(service.confirm(stageId, req, contentType));
  }
}
