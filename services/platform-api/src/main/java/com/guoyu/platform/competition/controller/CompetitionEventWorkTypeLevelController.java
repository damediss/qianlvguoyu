package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionEventWorkTypeLevelBatchReq;
import com.guoyu.platform.competition.service.CompetitionEventWorkTypeLevelApplicationService;
import com.guoyu.platform.competition.vo.CompetitionEventWorkTypeLevelVO;
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
@RequestMapping("/competition/events/{eventId}/work-types/{eventWorkTypeId}/levels")
public class CompetitionEventWorkTypeLevelController {

  private final CompetitionEventWorkTypeLevelApplicationService service;

  public CompetitionEventWorkTypeLevelController(
      CompetitionEventWorkTypeLevelApplicationService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('competition:stage:view')")
  public ApiResponse<List<CompetitionEventWorkTypeLevelVO>> list(
      @PathVariable Long eventId,
      @PathVariable Long eventWorkTypeId) {
    return ApiResponse.ok(service.list(eventId, eventWorkTypeId));
  }

  @PutMapping
  @PreAuthorize("hasAuthority('competition:stage:manage')")
  @AuditLog(
      module = "competition",
      action = "event-work-type-level:replace",
      targetType = "competition_event_work_type_level")
  public ApiResponse<List<CompetitionEventWorkTypeLevelVO>> replace(
      @PathVariable Long eventId,
      @PathVariable Long eventWorkTypeId,
      @Valid @RequestBody CompetitionEventWorkTypeLevelBatchReq req) {
    return ApiResponse.ok(service.replace(eventId, eventWorkTypeId, req));
  }
}
