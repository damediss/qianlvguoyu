package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionWorkTypeStageReplaceReq;
import com.guoyu.platform.competition.service.CompetitionWorkTypeStageApplicationService;
import com.guoyu.platform.competition.vo.CompetitionWorkTypeStageConfigVO;
import com.guoyu.platform.competition.vo.CompetitionEventWorkTypeVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/competition/events/{eventId}/work-types/{eventWorkTypeId}/stages")
public class CompetitionWorkTypeStageController {
  private final CompetitionWorkTypeStageApplicationService service;

  public CompetitionWorkTypeStageController(CompetitionWorkTypeStageApplicationService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('competition:event-work-type:view')")
  public ApiResponse<List<CompetitionWorkTypeStageConfigVO>> list(
      @PathVariable Long eventId,
      @PathVariable Long eventWorkTypeId,
      @RequestParam(required = false) String eventWorkTypeLevelCode) {
    return ApiResponse.ok(service.list(eventId, eventWorkTypeId, eventWorkTypeLevelCode));
  }

  @PutMapping
  @PreAuthorize("hasAuthority('competition:event-work-type:manage')")
  @AuditLog(module = "competition", action = "event-work-type:stages", targetType = "competition_event_work_type_stage")
  public ApiResponse<List<CompetitionWorkTypeStageConfigVO>> replace(
      @PathVariable Long eventId, @PathVariable Long eventWorkTypeId,
      @RequestParam(required = false) String eventWorkTypeLevelCode,
      @Valid @RequestBody CompetitionWorkTypeStageReplaceReq req) {
    return ApiResponse.ok(service.replace(eventId, eventWorkTypeId, eventWorkTypeLevelCode, req));
  }

  @PostMapping("/confirm")
  @PreAuthorize("hasAuthority('competition:event-work-type:manage')")
  @AuditLog(module = "competition", action = "event-work-type:stages-confirm", targetType = "competition_event_work_type")
  public ApiResponse<CompetitionEventWorkTypeVO> confirm(
      @PathVariable Long eventId,
      @PathVariable Long eventWorkTypeId,
      @RequestParam(required = false) String eventWorkTypeLevelCode) {
    return ApiResponse.ok(service.confirm(eventId, eventWorkTypeId, eventWorkTypeLevelCode));
  }
}
