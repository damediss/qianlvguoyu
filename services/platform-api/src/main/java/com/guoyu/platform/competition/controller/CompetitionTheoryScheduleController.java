package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.web.DownloadHeaders;
import com.guoyu.platform.competition.dto.CompetitionTheoryAssignmentAdjustReq;
import com.guoyu.platform.competition.dto.CompetitionTheoryRoomUpsertReq;
import com.guoyu.platform.competition.dto.CompetitionTheoryScheduleCreateReq;
import com.guoyu.platform.competition.dto.CompetitionTheoryScheduleWithdrawReq;
import com.guoyu.platform.competition.dto.CompetitionTheorySessionUpsertReq;
import com.guoyu.platform.competition.service.CompetitionTheoryScheduleApplicationService;
import com.guoyu.platform.competition.service.CompetitionTheoryRosterExportService;
import com.guoyu.platform.competition.vo.CompetitionTheoryRoomVO;
import com.guoyu.platform.competition.vo.CompetitionTheoryAssignmentAdjustmentVO;
import com.guoyu.platform.competition.vo.CompetitionTheoryScheduleVersionVO;
import com.guoyu.platform.competition.vo.CompetitionTheorySchedulePreflightVO;
import com.guoyu.platform.competition.vo.CompetitionTheorySessionVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
public class CompetitionTheoryScheduleController {

  private final CompetitionTheoryScheduleApplicationService service;
  private final CompetitionTheoryRosterExportService exportService;

  public CompetitionTheoryScheduleController(
      CompetitionTheoryScheduleApplicationService service,
      CompetitionTheoryRosterExportService exportService) {
    this.service = service;
    this.exportService = exportService;
  }

  @PostMapping("/events/{eventId}/theory-schedules")
  @PreAuthorize("hasAuthority('competition:theory-schedule:manage')")
  @AuditLog(module = "competition", action = "theory-schedule:create",
      targetType = "competition_theory_schedule_version")
  public ApiResponse<CompetitionTheoryScheduleVersionVO> createVersion(
      @PathVariable Long eventId,
      @Valid @RequestBody CompetitionTheoryScheduleCreateReq req) {
    return ApiResponse.ok(service.createVersion(eventId, req));
  }

  @GetMapping("/events/{eventId}/theory-schedules")
  @PreAuthorize("hasAuthority('competition:theory-schedule:view')")
  public ApiResponse<List<CompetitionTheoryScheduleVersionVO>> listVersions(
      @PathVariable Long eventId,
      @RequestParam(required = false) Long stageId,
      @RequestParam(required = false) Long eventWorkTypeId) {
    return ApiResponse.ok(service.listVersions(eventId, stageId, eventWorkTypeId));
  }

  @GetMapping("/theory-schedules/{versionId}")
  @PreAuthorize("hasAuthority('competition:theory-schedule:view')")
  public ApiResponse<CompetitionTheoryScheduleVersionVO> detail(
      @PathVariable Long versionId) {
    return ApiResponse.ok(service.detail(versionId));
  }

  @GetMapping("/theory-schedules/{versionId}/preflight")
  @PreAuthorize("hasAuthority('competition:theory-schedule:view')")
  public ApiResponse<CompetitionTheorySchedulePreflightVO> preflight(
      @PathVariable Long versionId) {
    return ApiResponse.ok(service.preflight(versionId));
  }

  @GetMapping("/theory-schedules/{versionId}/adjustments")
  @PreAuthorize("hasAuthority('competition:theory-schedule:view')")
  public ApiResponse<List<CompetitionTheoryAssignmentAdjustmentVO>> listAdjustments(
      @PathVariable Long versionId) {
    return ApiResponse.ok(service.listAdjustments(versionId));
  }

  @GetMapping("/events/{eventId}/theory-exam-rooms")
  @PreAuthorize("hasAuthority('competition:theory-schedule:view')")
  public ApiResponse<List<CompetitionTheoryRoomVO>> listRooms(@PathVariable Long eventId) {
    return ApiResponse.ok(service.listRooms(eventId));
  }

  @PostMapping("/events/{eventId}/theory-exam-rooms")
  @PreAuthorize("hasAuthority('competition:theory-schedule:manage')")
  @AuditLog(module = "competition", action = "theory-room:create",
      targetType = "competition_theory_exam_room")
  public ApiResponse<CompetitionTheoryRoomVO> createRoom(
      @PathVariable Long eventId,
      @Valid @RequestBody CompetitionTheoryRoomUpsertReq req) {
    return ApiResponse.ok(service.createRoom(eventId, req));
  }

  @PutMapping("/theory-exam-rooms/{roomId}")
  @PreAuthorize("hasAuthority('competition:theory-schedule:manage')")
  @AuditLog(module = "competition", action = "theory-room:update",
      targetType = "competition_theory_exam_room")
  public ApiResponse<CompetitionTheoryRoomVO> updateRoom(
      @PathVariable Long roomId,
      @Valid @RequestBody CompetitionTheoryRoomUpsertReq req) {
    return ApiResponse.ok(service.updateRoom(roomId, req));
  }

  @DeleteMapping("/theory-exam-rooms/{roomId}")
  @PreAuthorize("hasAuthority('competition:theory-schedule:manage')")
  @AuditLog(module = "competition", action = "theory-room:delete",
      targetType = "competition_theory_exam_room")
  public ApiResponse<Void> deleteRoom(@PathVariable Long roomId) {
    service.deleteRoom(roomId);
    return ApiResponse.ok();
  }

  @PostMapping("/theory-schedules/{versionId}/sessions")
  @PreAuthorize("hasAuthority('competition:theory-schedule:manage')")
  @AuditLog(module = "competition", action = "theory-session:create",
      targetType = "competition_theory_exam_session")
  public ApiResponse<CompetitionTheorySessionVO> createSession(
      @PathVariable Long versionId,
      @Valid @RequestBody CompetitionTheorySessionUpsertReq req) {
    return ApiResponse.ok(service.createSession(versionId, req));
  }

  @PutMapping("/theory-schedules/{versionId}/sessions/{sessionId}")
  @PreAuthorize("hasAuthority('competition:theory-schedule:manage')")
  @AuditLog(module = "competition", action = "theory-session:update",
      targetType = "competition_theory_exam_session")
  public ApiResponse<CompetitionTheorySessionVO> updateSession(
      @PathVariable Long versionId,
      @PathVariable Long sessionId,
      @Valid @RequestBody CompetitionTheorySessionUpsertReq req) {
    return ApiResponse.ok(service.updateSession(versionId, sessionId, req));
  }

  @DeleteMapping("/theory-schedules/{versionId}/sessions/{sessionId}")
  @PreAuthorize("hasAuthority('competition:theory-schedule:manage')")
  @AuditLog(module = "competition", action = "theory-session:delete",
      targetType = "competition_theory_exam_session")
  public ApiResponse<Void> deleteSession(
      @PathVariable Long versionId, @PathVariable Long sessionId) {
    service.deleteSession(versionId, sessionId);
    return ApiResponse.ok();
  }

  @PostMapping("/theory-schedules/{versionId}/random-allocate")
  @PreAuthorize("hasAuthority('competition:theory-schedule:manage')")
  @AuditLog(module = "competition", action = "theory-schedule:random-allocate",
      targetType = "competition_theory_candidate_assignment")
  public ApiResponse<CompetitionTheoryScheduleVersionVO> randomAllocate(
      @PathVariable Long versionId) {
    return ApiResponse.ok(service.randomAllocate(versionId));
  }

  @PutMapping("/theory-assignments/{assignmentId}/adjust")
  @PreAuthorize("hasAuthority('competition:theory-schedule:manage')")
  @AuditLog(module = "competition", action = "theory-assignment:adjust",
      targetType = "competition_theory_candidate_assignment")
  public ApiResponse<CompetitionTheoryScheduleVersionVO> adjust(
      @PathVariable Long assignmentId,
      @Valid @RequestBody CompetitionTheoryAssignmentAdjustReq req) {
    return ApiResponse.ok(service.adjust(assignmentId, req));
  }

  @PostMapping("/theory-schedules/{versionId}/publish")
  @PreAuthorize("hasAuthority('competition:theory-schedule:publish')")
  @AuditLog(module = "competition", action = "theory-schedule:publish",
      targetType = "competition_theory_schedule_version")
  public ApiResponse<CompetitionTheoryScheduleVersionVO> publish(
      @PathVariable Long versionId) {
    return ApiResponse.ok(service.publish(versionId));
  }

  @PostMapping("/theory-schedules/{versionId}/withdraw")
  @PreAuthorize("hasAuthority('competition:theory-schedule:publish')")
  @AuditLog(module = "competition", action = "theory-schedule:withdraw",
      targetType = "competition_theory_schedule_version")
  public ApiResponse<CompetitionTheoryScheduleVersionVO> withdraw(
      @PathVariable Long versionId,
      @Valid @RequestBody CompetitionTheoryScheduleWithdrawReq req) {
    return ApiResponse.ok(service.withdraw(versionId, req.reason()));
  }

  @GetMapping("/theory-schedules/{versionId}/roster.xlsx")
  @PreAuthorize("hasAuthority('competition:theory-roster:export')")
  public ResponseEntity<byte[]> exportExcel(@PathVariable Long versionId) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            DownloadHeaders.contentDispositionAttachment("competition-theory-roster.xlsx"))
        .body(exportService.exportExcel(versionId));
  }

  @GetMapping("/theory-schedules/{versionId}/roster.pdf")
  @PreAuthorize("hasAuthority('competition:theory-roster:export')")
  public ResponseEntity<byte[]> exportPdf(@PathVariable Long versionId) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            DownloadHeaders.contentDispositionAttachment("competition-theory-roster.pdf"))
        .body(exportService.exportPdf(versionId));
  }
}
