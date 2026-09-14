package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionFormalExamPlayerAnswerSaveReq;
import com.guoyu.platform.competition.dto.CompetitionFormalExamPlayerAnswerSyncReq;
import com.guoyu.platform.competition.dto.CompetitionFormalExamPlayerLoginReq;
import com.guoyu.platform.competition.dto.CompetitionFormalExamPlayerRealtimeAckReq;
import com.guoyu.platform.competition.dto.CompetitionFormalExamPlayerSubmitReq;
import com.guoyu.platform.competition.exam.realtime.FormalExamPlayerRealtimeApplicationService;
import com.guoyu.platform.competition.service.CompetitionFormalExamPlayerApplicationService;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerAnswerSaveVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerAnswerSyncVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerContextVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerLoginVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerPaperVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerRealtimeTicketVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerStatusVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerSubmitVO;
import com.guoyu.platform.competition.vo.FormalExamRealtimeCatchUpVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/competition/formal-exam/player")
public class CompetitionFormalExamPlayerController {

  private final CompetitionFormalExamPlayerApplicationService service;
  private final FormalExamPlayerRealtimeApplicationService realtime;

  public CompetitionFormalExamPlayerController(
      CompetitionFormalExamPlayerApplicationService service) {
    this(service, null);
  }

  @Autowired
  public CompetitionFormalExamPlayerController(
      CompetitionFormalExamPlayerApplicationService service,
      FormalExamPlayerRealtimeApplicationService realtime) {
    this.service = service;
    this.realtime = realtime;
  }

  @PostMapping("/auth/login")
  public ApiResponse<CompetitionFormalExamPlayerLoginVO> login(
      @Valid @RequestBody CompetitionFormalExamPlayerLoginReq request) {
    return ApiResponse.ok(service.login(request));
  }

  @PostMapping("/auth/logout")
  @PreAuthorize("hasRole('COMPETITION_EXAM')")
  public ApiResponse<Void> logout() {
    service.logout();
    return ApiResponse.ok();
  }

  @PostMapping("/realtime/ticket")
  @PreAuthorize("hasRole('COMPETITION_EXAM')")
  public ApiResponse<CompetitionFormalExamPlayerRealtimeTicketVO> realtimeTicket() {
    return ApiResponse.ok(requireRealtime().issueTicket());
  }

  @PostMapping("/realtime/ack")
  @PreAuthorize("hasRole('COMPETITION_EXAM')")
  public ApiResponse<Void> realtimeAck(
      @Valid @RequestBody CompetitionFormalExamPlayerRealtimeAckReq request) {
    requireRealtime().ackFromPlayer(request.commandId());
    return ApiResponse.ok();
  }

  @GetMapping("/realtime/events")
  @PreAuthorize("hasRole('COMPETITION_EXAM')")
  public ApiResponse<FormalExamRealtimeCatchUpVO> realtimeEvents(
      @RequestParam(required = false) String afterSeq,
      @RequestParam(required = false) Integer limit) {
    return ApiResponse.ok(requireRealtime().catchUp(afterSeq, limit));
  }

  @GetMapping("/sessions/{sessionId}/context")
  @PreAuthorize("hasRole('COMPETITION_EXAM')")
  public ApiResponse<CompetitionFormalExamPlayerContextVO> context(
      @PathVariable String sessionId) {
    return ApiResponse.ok(service.context(sessionId));
  }

  @GetMapping("/sessions/{sessionId}/status")
  @PreAuthorize("hasRole('COMPETITION_EXAM')")
  public ApiResponse<CompetitionFormalExamPlayerStatusVO> status(
      @PathVariable String sessionId) {
    return ApiResponse.ok(service.status(sessionId));
  }

  @PostMapping("/sessions/{sessionId}/enter")
  @PreAuthorize("hasRole('COMPETITION_EXAM')")
  public ApiResponse<CompetitionFormalExamPlayerContextVO> enter(
      @PathVariable String sessionId) {
    return ApiResponse.ok(service.enter(sessionId));
  }

  @GetMapping("/sessions/{sessionId}/paper")
  @PreAuthorize("hasRole('COMPETITION_EXAM')")
  public ApiResponse<CompetitionFormalExamPlayerPaperVO> paper(
      @PathVariable String sessionId) {
    return ApiResponse.ok(service.paper(sessionId));
  }

  @PutMapping("/sessions/{sessionId}/questions/{questionSnapshotId}/answer")
  @PreAuthorize("hasRole('COMPETITION_EXAM')")
  public ApiResponse<CompetitionFormalExamPlayerAnswerSaveVO> saveAnswer(
      @PathVariable String sessionId,
      @PathVariable String questionSnapshotId,
      @RequestBody(required = false) CompetitionFormalExamPlayerAnswerSaveReq request) {
    return ApiResponse.ok(service.saveAnswer(sessionId, questionSnapshotId, request));
  }

  @PostMapping("/sessions/{sessionId}/answers/sync")
  @PreAuthorize("hasRole('COMPETITION_EXAM')")
  public ApiResponse<CompetitionFormalExamPlayerAnswerSyncVO> syncAnswers(
      @PathVariable String sessionId,
      @RequestBody(required = false) CompetitionFormalExamPlayerAnswerSyncReq request) {
    return ApiResponse.ok(service.syncAnswers(sessionId, request));
  }

  @PostMapping("/sessions/{sessionId}/submit")
  @PreAuthorize("hasRole('COMPETITION_EXAM')")
  public ApiResponse<CompetitionFormalExamPlayerSubmitVO> submit(
      @PathVariable String sessionId,
      @RequestBody(required = false) CompetitionFormalExamPlayerSubmitReq request) {
    return ApiResponse.ok(service.submit(sessionId, request));
  }

  private FormalExamPlayerRealtimeApplicationService requireRealtime() {
    if (realtime == null) {
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "实时通道暂时不可用");
    }
    return realtime;
  }
}
