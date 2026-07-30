package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionContestantWorkTypeConfirmReq;
import com.guoyu.platform.competition.service.CompetitionContestantWorkTypeApplicationService;
import com.guoyu.platform.competition.vo.CompetitionContestantWorkTypeReconciliationVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/competition/events/{eventId}")
public class CompetitionContestantWorkTypeController {

  private final CompetitionContestantWorkTypeApplicationService service;

  public CompetitionContestantWorkTypeController(
      CompetitionContestantWorkTypeApplicationService service) {
    this.service = service;
  }

  @GetMapping("/work-type-reconciliation")
  @PreAuthorize("hasAuthority('competition:event-work-type:view')")
  public ApiResponse<List<CompetitionContestantWorkTypeReconciliationVO>> reconcile(
      @PathVariable Long eventId) {
    return ApiResponse.ok(service.reconcile(eventId));
  }

  @PutMapping("/contestants/{contestantMemberId}/work-type")
  @PreAuthorize("hasAuthority('competition:event-work-type:manage')")
  @AuditLog(
      module = "competition",
      action = "contestant-work-type:confirm",
      targetType = "competition_contestant_profile")
  public ApiResponse<CompetitionContestantWorkTypeReconciliationVO> confirm(
      @PathVariable Long eventId,
      @PathVariable Long contestantMemberId,
      @Valid @RequestBody CompetitionContestantWorkTypeConfirmReq req) {
    return ApiResponse.ok(service.confirm(eventId, contestantMemberId, req));
  }
}
