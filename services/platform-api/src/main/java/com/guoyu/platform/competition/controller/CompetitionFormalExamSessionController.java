package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionFormalCandidateExamQueryReq;
import com.guoyu.platform.competition.dto.CompetitionFormalExamForceSubmitReq;
import com.guoyu.platform.competition.dto.CompetitionFormalExamSessionStartReq;
import com.guoyu.platform.competition.exam.realtime.FormalExamConsoleRealtimeApplicationService;
import com.guoyu.platform.competition.service.CompetitionFormalExamSessionApplicationService;
import com.guoyu.platform.competition.service.CompetitionFormalExamScoringService;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateExamVO;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateResultVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamConsoleRealtimeTicketVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPreflightVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamSessionVO;
import com.guoyu.platform.competition.vo.FormalExamRealtimeCatchUpVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class CompetitionFormalExamSessionController {

  private final CompetitionFormalExamSessionApplicationService service;
  private final CompetitionFormalExamScoringService scoringService;
  private final FormalExamConsoleRealtimeApplicationService consoleRealtime;

  public CompetitionFormalExamSessionController(
      CompetitionFormalExamSessionApplicationService service,
      CompetitionFormalExamScoringService scoringService) {
    this(service, scoringService, null);
  }

  @Autowired
  public CompetitionFormalExamSessionController(
      CompetitionFormalExamSessionApplicationService service,
      CompetitionFormalExamScoringService scoringService,
      FormalExamConsoleRealtimeApplicationService consoleRealtime) {
    this.service = service;
    this.scoringService = scoringService;
    this.consoleRealtime = consoleRealtime;
  }

  @PostMapping("/competition/stages/{stageId}/formal-exam-sessions/prepare")
  @PreAuthorize("hasAuthority('competition:exam-session:prepare')")
  @AuditLog(module = "competition", action = "exam-session:prepare",
      targetType = "competition_formal_exam_session")
  public ApiResponse<CompetitionFormalExamSessionVO> prepare(
      @PathVariable Long stageId,
      @RequestParam(required = false) String examType) {
    return ApiResponse.ok(service.prepareExamSession(stageId, examType));
  }

  @GetMapping("/competition/stages/{stageId}/formal-exam-sessions")
  @PreAuthorize("hasAuthority('competition:exam-session:view')")
  public ApiResponse<CompetitionFormalExamSessionVO> byStage(
      @PathVariable Long stageId,
      @RequestParam(required = false) String examType) {
    return ApiResponse.ok(service.getByStage(stageId, examType));
  }

  @GetMapping("/competition/formal-exam-sessions/{id}")
  @PreAuthorize("hasAuthority('competition:exam-session:view')")
  public ApiResponse<CompetitionFormalExamSessionVO> get(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  @GetMapping("/competition/formal-exam-sessions/{id}/candidates")
  @PreAuthorize("hasAuthority('competition:exam-session:view')")
  public ApiResponse<PageResponse<CompetitionFormalCandidateExamVO>> candidates(
      @PathVariable Long id,
      @Valid @ModelAttribute CompetitionFormalCandidateExamQueryReq req) {
    return ApiResponse.ok(service.pageCandidates(id, req));
  }

  @PostMapping("/competition/formal-exam-sessions/{id}/realtime/ticket")
  @PreAuthorize("hasAuthority('competition:exam-session:view')")
  public ApiResponse<CompetitionFormalExamConsoleRealtimeTicketVO> realtimeTicket(
      @PathVariable Long id) {
    return ApiResponse.ok(requireConsoleRealtime().issueTicket(id));
  }

  @GetMapping("/competition/formal-exam-sessions/{sessionId}/realtime/events")
  @PreAuthorize("hasAuthority('competition:exam-session:view')")
  public ApiResponse<FormalExamRealtimeCatchUpVO> realtimeEvents(
      @PathVariable Long sessionId,
      @RequestParam(required = false) String afterSeq,
      @RequestParam(required = false) Integer limit) {
    return ApiResponse.ok(requireConsoleRealtime().catchUp(sessionId, afterSeq, limit));
  }

  @GetMapping("/competition/formal-exam-sessions/{id}/preflight")
  @PreAuthorize("hasAuthority('competition:exam-session:view')")
  public ApiResponse<CompetitionFormalExamPreflightVO> preflight(@PathVariable Long id) {
    return ApiResponse.ok(service.getPreflight(id));
  }

  @PostMapping("/competition/formal-exam-sessions/{id}/start")
  @PreAuthorize("hasAuthority('competition:exam-session:start')")
  @AuditLog(module = "competition", action = "exam-session:start",
      targetType = "competition_formal_exam_session")
  public ApiResponse<CompetitionFormalExamSessionVO> start(
      @PathVariable Long id,
      @RequestBody(required = false) CompetitionFormalExamSessionStartReq req) {
    return ApiResponse.ok(service.startExamSession(id, req));
  }

  @PostMapping("/competition/formal-exam-sessions/{sessionId}/candidates/{candidateExamId}/force-submit")
  @PreAuthorize("hasAuthority('competition:exam-session:force-submit')")
  @AuditLog(module = "competition", action = "exam-session:force-submit",
      targetType = "competition_formal_candidate_exam")
  public ApiResponse<CompetitionFormalCandidateExamVO> forceSubmit(
      @PathVariable Long sessionId,
      @PathVariable Long candidateExamId,
      @Valid @RequestBody CompetitionFormalExamForceSubmitReq request) {
    return ApiResponse.ok(service.forceSubmit(
        sessionId, candidateExamId, request == null ? null : request.reason()));
  }

  @PostMapping("/competition/formal-exam-sessions/{sessionId}/candidates/{candidateExamId}/force-offline")
  @PreAuthorize("hasAuthority('competition:exam-session:force-offline')")
  @AuditLog(module = "competition", action = "exam-session:force-offline",
      targetType = "competition_formal_candidate_exam")
  public ApiResponse<Void> forceOffline(
      @PathVariable Long sessionId,
      @PathVariable Long candidateExamId) {
    service.forceOffline(sessionId, candidateExamId);
    return ApiResponse.ok();
  }

  @PostMapping("/competition/formal-exam-sessions/{id}/score")
  @PreAuthorize("hasAuthority('competition:exam-session:score')")
  @AuditLog(module = "competition", action = "exam-session:score",
      targetType = "competition_formal_exam_session")
  public ApiResponse<CompetitionFormalExamSessionVO> score(@PathVariable Long id) {
    scoringService.retryFromAdmin(id);
    return ApiResponse.ok(service.getById(id));
  }

  @GetMapping("/competition/formal-exam-candidates/{candidateExamId}/result")
  @PreAuthorize("hasAuthority('competition:exam-session:view')")
  public ApiResponse<CompetitionFormalCandidateResultVO> candidateResult(
      @PathVariable Long candidateExamId) {
    return ApiResponse.ok(service.getCandidateResult(candidateExamId));
  }

  @GetMapping("/competition/formal-exam-candidates/{candidateExamId}")
  @PreAuthorize("hasAuthority('competition:exam-session:view')")
  public ApiResponse<CompetitionFormalCandidateExamVO> candidate(
      @PathVariable Long candidateExamId) {
    return ApiResponse.ok(service.getCandidate(candidateExamId));
  }

  private FormalExamConsoleRealtimeApplicationService requireConsoleRealtime() {
    if (consoleRealtime == null) {
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "实时通道暂时不可用");
    }
    return consoleRealtime;
  }
}
