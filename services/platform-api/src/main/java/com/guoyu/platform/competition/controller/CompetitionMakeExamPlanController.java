package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionMakeExamPlanQueryReq;
import com.guoyu.platform.competition.dto.CompetitionMakeExamPlanUpsertReq;
import com.guoyu.platform.competition.service.CompetitionMakeExamPlanApplicationService;
import com.guoyu.platform.competition.vo.CompetitionMakeExamPlanVO;
import com.guoyu.platform.competition.vo.CompetitionMakeExamSnapshotVO;
import com.guoyu.platform.competition.vo.CompetitionMakeExamPlanPrecheckVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
public class CompetitionMakeExamPlanController {

  private final CompetitionMakeExamPlanApplicationService service;

  public CompetitionMakeExamPlanController(CompetitionMakeExamPlanApplicationService service) {
    this.service = service;
  }

  @GetMapping("/events/{eventId}/make-exam-plans")
  @PreAuthorize("hasAuthority('competition:make-plan:view')")
  public ApiResponse<PageResponse<CompetitionMakeExamPlanVO>> page(
      @PathVariable Long eventId, @Valid @ModelAttribute CompetitionMakeExamPlanQueryReq req) {
    return ApiResponse.ok(service.page(eventId, req));
  }

  @GetMapping("/events/{eventId}/make-exam-plans/{planId}")
  @PreAuthorize("hasAuthority('competition:make-plan:view')")
  public ApiResponse<CompetitionMakeExamPlanVO> get(@PathVariable Long eventId, @PathVariable Long planId) {
    return ApiResponse.ok(service.get(eventId, planId));
  }

  @PostMapping("/events/{eventId}/make-exam-plans")
  @PreAuthorize("hasAuthority('competition:make-plan:create')")
  @AuditLog(module = "competition", action = "make-plan:create", targetType = "competition_make_exam_plan")
  public ApiResponse<CompetitionMakeExamPlanVO> create(
      @PathVariable Long eventId, @Valid @RequestBody CompetitionMakeExamPlanUpsertReq req) {
    return ApiResponse.ok(service.create(eventId, req));
  }

  @PostMapping("/events/{eventId}/make-exam-plans/precheck")
  @PreAuthorize("hasAuthority('competition:make-plan:view')")
  public ApiResponse<CompetitionMakeExamPlanPrecheckVO> precheck(
      @PathVariable Long eventId, @Valid @RequestBody CompetitionMakeExamPlanUpsertReq req) {
    return ApiResponse.ok(service.precheck(eventId, req));
  }

  @PutMapping("/events/{eventId}/make-exam-plans/{planId}")
  @PreAuthorize("hasAuthority('competition:make-plan:update')")
  @AuditLog(module = "competition", action = "make-plan:update", targetType = "competition_make_exam_plan")
  public ApiResponse<CompetitionMakeExamPlanVO> update(
      @PathVariable Long eventId, @PathVariable Long planId,
      @Valid @RequestBody CompetitionMakeExamPlanUpsertReq req) {
    return ApiResponse.ok(service.update(eventId, planId, req));
  }

  @DeleteMapping("/events/{eventId}/make-exam-plans/{planId}")
  @PreAuthorize("hasAuthority('competition:make-plan:delete')")
  @AuditLog(module = "competition", action = "make-plan:delete", targetType = "competition_make_exam_plan")
  public ApiResponse<Void> delete(@PathVariable Long eventId, @PathVariable Long planId) {
    service.delete(eventId, planId);
    return ApiResponse.ok();
  }

  @PostMapping("/events/{eventId}/make-exam-plans/{planId}/publish")
  @PreAuthorize("hasAuthority('competition:make-plan:publish')")
  @AuditLog(module = "competition", action = "make-plan:publish", targetType = "competition_make_exam_plan")
  public ApiResponse<CompetitionMakeExamPlanVO> publish(@PathVariable Long eventId, @PathVariable Long planId) {
    return ApiResponse.ok(service.publish(eventId, planId));
  }

  @GetMapping("/events/{eventId}/make-exam-plans/{planId}/snapshots")
  @PreAuthorize("hasAuthority('competition:make-plan:view')")
  public ApiResponse<List<CompetitionMakeExamSnapshotVO>> snapshots(@PathVariable Long eventId, @PathVariable Long planId) {
    return ApiResponse.ok(service.listSnapshots(eventId, planId));
  }

  @GetMapping("/events/{eventId}/make-exam-snapshots")
  @PreAuthorize("hasAnyAuthority('competition:make-plan:view', 'competition:theory-paper:view')")
  public ApiResponse<List<CompetitionMakeExamSnapshotVO>> publishedSnapshots(
      @PathVariable Long eventId,
      @RequestParam Long eventWorkTypeId,
      @RequestParam Long eventWorkTypeLevelId,
      @RequestParam(defaultValue = "theory") String contentType) {
    return ApiResponse.ok(service.listPublishedSnapshots(
        eventId, eventWorkTypeId, eventWorkTypeLevelId, contentType));
  }

  @GetMapping("/events/{eventId}/make-exam-plan-snapshots/{snapshotId}/preview")
  @PreAuthorize("hasAuthority('competition:make-plan:view')")
  public ApiResponse<CompetitionMakeExamSnapshotVO> preview(
      @PathVariable Long eventId, @PathVariable Long snapshotId) {
    return ApiResponse.ok(service.previewSnapshot(eventId, snapshotId));
  }
}
