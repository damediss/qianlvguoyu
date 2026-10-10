package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionEventWorkTypeCreateReq;
import com.guoyu.platform.competition.dto.CompetitionEventWorkTypeUpdateReq;
import com.guoyu.platform.competition.service.CompetitionEventWorkTypeApplicationService;
import com.guoyu.platform.competition.vo.CompetitionEventWorkTypeVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/competition/events/{eventId}/work-types")
public class CompetitionEventWorkTypeController {

  private final CompetitionEventWorkTypeApplicationService service;

  public CompetitionEventWorkTypeController(
      CompetitionEventWorkTypeApplicationService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('competition:event-work-type:view')")
  public ApiResponse<List<CompetitionEventWorkTypeVO>> list(
      @PathVariable Long eventId) {
    return ApiResponse.ok(service.list(eventId));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('competition:event-work-type:manage')")
  @AuditLog(
      module = "competition",
      action = "event-work-type:create",
      targetType = "competition_event_work_type")
  public ApiResponse<CompetitionEventWorkTypeVO> create(
      @PathVariable Long eventId,
      @Valid @RequestBody CompetitionEventWorkTypeCreateReq req) {
    return ApiResponse.ok(service.create(eventId, req));
  }

  @PutMapping("/{eventWorkTypeId}")
  @PreAuthorize("hasAuthority('competition:event-work-type:manage')")
  @AuditLog(
      module = "competition",
      action = "event-work-type:update",
      targetType = "competition_event_work_type")
  public ApiResponse<CompetitionEventWorkTypeVO> update(
      @PathVariable Long eventId,
      @PathVariable Long eventWorkTypeId,
      @Valid @RequestBody CompetitionEventWorkTypeUpdateReq req) {
    return ApiResponse.ok(service.update(eventId, eventWorkTypeId, req));
  }

  @DeleteMapping("/{eventWorkTypeId}")
  @PreAuthorize("hasAuthority('competition:event-work-type:manage')")
  @AuditLog(
      module = "competition",
      action = "event-work-type:delete",
      targetType = "competition_event_work_type")
  public ApiResponse<Void> delete(
      @PathVariable Long eventId,
      @PathVariable Long eventWorkTypeId) {
    service.delete(eventId, eventWorkTypeId);
    return ApiResponse.ok(null);
  }
}
