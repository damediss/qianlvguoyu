package com.guoyu.platform.competition.prepractice.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.prepractice.dto.CompetitionPrePracticeQuestionCountReq;
import com.guoyu.platform.competition.prepractice.dto.CompetitionPrePracticeAvailableTaskQueryReq;
import com.guoyu.platform.competition.prepractice.dto.CompetitionPrePracticeContestantQueryReq;
import com.guoyu.platform.competition.prepractice.dto.CompetitionPrePracticeDispatchCreateReq;
import com.guoyu.platform.competition.prepractice.dto.CompetitionPrePracticeDispatchQueryReq;
import com.guoyu.platform.competition.prepractice.dto.CompetitionPrePracticeTaskCreateReq;
import com.guoyu.platform.competition.prepractice.dto.CompetitionPrePracticeTaskQueryReq;
import com.guoyu.platform.competition.prepractice.service.CompetitionPrePracticeDispatchApplicationService;
import com.guoyu.platform.competition.prepractice.service.CompetitionPrePracticeTaskApplicationService;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPrePracticeContestantVO;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPrePracticeDispatchBatchVO;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPrePracticeOrganizationVO;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPrePracticeQuestionTypeCountVO;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPrePracticeTaskVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/competition/pre-practice")
public class CompetitionPrePracticeController {

  private final CompetitionPrePracticeTaskApplicationService service;
  private final CompetitionPrePracticeDispatchApplicationService dispatchService;

  public CompetitionPrePracticeController(
      CompetitionPrePracticeTaskApplicationService service,
      CompetitionPrePracticeDispatchApplicationService dispatchService) {
    this.service = service;
    this.dispatchService = dispatchService;
  }

  @GetMapping("/tasks")
  @PreAuthorize("hasAuthority('competition:player-pre-practice:view')")
  public ApiResponse<PageResponse<CompetitionPrePracticeTaskVO>> page(
      @Valid @ModelAttribute CompetitionPrePracticeTaskQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/available-tasks")
  @PreAuthorize("hasAuthority('competition:player-pre-practice:view')")
  public ApiResponse<PageResponse<CompetitionPrePracticeTaskVO>> availableForPush(
      @Valid @ModelAttribute CompetitionPrePracticeAvailableTaskQueryReq req) {
    return ApiResponse.ok(service.availableForPush(req));
  }

  @GetMapping("/push-contestants")
  @PreAuthorize("hasAuthority('competition:player-pre-practice:view')")
  public ApiResponse<PageResponse<CompetitionPrePracticeContestantVO>> contestants(
      @Valid @ModelAttribute CompetitionPrePracticeContestantQueryReq req) {
    return ApiResponse.ok(dispatchService.pageContestants(req));
  }

  @GetMapping("/push-organizations")
  @PreAuthorize("hasAuthority('competition:player-pre-practice:view')")
  public ApiResponse<List<CompetitionPrePracticeOrganizationVO>> organizations(
      @Valid @ModelAttribute CompetitionPrePracticeContestantQueryReq req) {
    return ApiResponse.ok(dispatchService.organizations(req));
  }

  @GetMapping("/dispatches")
  @PreAuthorize("hasAuthority('competition:player-pre-practice:view')")
  public ApiResponse<PageResponse<CompetitionPrePracticeDispatchBatchVO>> dispatches(
      @Valid @ModelAttribute CompetitionPrePracticeDispatchQueryReq req) {
    return ApiResponse.ok(dispatchService.pageDispatches(req));
  }

  @PostMapping("/dispatches")
  @PreAuthorize("hasAuthority('competition:player-pre-practice:push')")
  @AuditLog(module = "competition", action = "pre-practice:push",
      targetType = "competition_pre_practice_dispatch_batch", includeResult = true)
  public ApiResponse<CompetitionPrePracticeDispatchBatchVO> dispatch(
      @Valid @RequestBody CompetitionPrePracticeDispatchCreateReq req) {
    return ApiResponse.ok(dispatchService.dispatch(req));
  }

  @PostMapping("/question-counts")
  @PreAuthorize("hasAuthority('competition:player-pre-practice:view')"
      + " and hasAuthority('exam:work-type:view')"
      + " and hasAuthority('exam:course:view')"
      + " and hasAuthority('exam:question:view')")
  public ApiResponse<List<CompetitionPrePracticeQuestionTypeCountVO>> questionCounts(
      @Valid @RequestBody CompetitionPrePracticeQuestionCountReq req) {
    return ApiResponse.ok(service.questionCounts(req));
  }

  @PostMapping("/tasks")
  @PreAuthorize("hasAuthority('competition:player-pre-practice:create')"
      + " and hasAuthority('exam:work-type:view')"
      + " and hasAuthority('exam:course:view')"
      + " and hasAuthority('exam:question:view')")
  @AuditLog(module = "competition", action = "pre-practice:create",
      targetType = "competition_pre_practice_task", includeResult = true)
  public ApiResponse<CompetitionPrePracticeTaskVO> create(
      @Valid @RequestBody CompetitionPrePracticeTaskCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }
}
