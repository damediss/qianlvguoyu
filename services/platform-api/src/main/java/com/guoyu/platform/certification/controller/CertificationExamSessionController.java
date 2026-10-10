package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.dto.CertificationCandidateExamSessionSaveReq;
import com.guoyu.platform.certification.dto.CertificationExamSessionCandidateAssignReq;
import com.guoyu.platform.certification.dto.CertificationExamSessionSaveReq;
import com.guoyu.platform.certification.service.CertificationExamSessionApplicationService;
import com.guoyu.platform.certification.vo.CertificationCandidateExamSessionVO;
import com.guoyu.platform.certification.vo.CertificationExamSessionCandidateVO;
import com.guoyu.platform.certification.vo.CertificationExamSessionVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
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
@RequestMapping("/certification")
public class CertificationExamSessionController {

  private final CertificationExamSessionApplicationService service;

  public CertificationExamSessionController(CertificationExamSessionApplicationService service) {
    this.service = service;
  }

  @PostMapping("/tasks/{taskId}/exam-sessions")
  @PreAuthorize("hasAuthority('certification.preparation.exam-session.edit')")
  @AuditLog(module = "certification", action = "exam-session:create", targetType = "certification_exam_session")
  public ApiResponse<CertificationExamSessionVO> create(
      @PathVariable Long taskId, @Valid @RequestBody CertificationExamSessionSaveReq req) {
    return ApiResponse.ok(service.create(taskId, req));
  }

  @GetMapping("/tasks/{taskId}/exam-sessions")
  @PreAuthorize("hasAuthority('certification.preparation.exam-session.view')")
  public ApiResponse<List<CertificationExamSessionVO>> list(@PathVariable Long taskId) {
    return ApiResponse.ok(service.list(taskId));
  }

  @PutMapping("/exam-sessions/{sessionId}")
  @PreAuthorize("hasAuthority('certification.preparation.exam-session.edit')")
  @AuditLog(module = "certification", action = "exam-session:update", targetType = "certification_exam_session")
  public ApiResponse<CertificationExamSessionVO> update(
      @PathVariable Long sessionId, @Valid @RequestBody CertificationExamSessionSaveReq req) {
    return ApiResponse.ok(service.update(sessionId, req));
  }

  @DeleteMapping("/exam-sessions/{sessionId}")
  @PreAuthorize("hasAuthority('certification.preparation.exam-session.delete')")
  @AuditLog(module = "certification", action = "exam-session:delete", targetType = "certification_exam_session")
  public ApiResponse<Void> delete(@PathVariable Long sessionId) {
    service.delete(sessionId);
    return ApiResponse.ok();
  }

  @GetMapping("/candidates/{candidateId}/exam-sessions")
  @PreAuthorize("hasAuthority('certification.preparation.candidate.view')")
  public ApiResponse<List<CertificationCandidateExamSessionVO>> listCandidateSessions(@PathVariable Long candidateId) {
    return ApiResponse.ok(service.listCandidateSessions(candidateId));
  }

  @PutMapping("/candidates/{candidateId}/exam-sessions/{sessionId}")
  @PreAuthorize("hasAuthority('certification.preparation.candidate.exam-session.edit')")
  @AuditLog(module = "certification", action = "candidate-exam-session:update", targetType = "certification_candidate_exam_session")
  public ApiResponse<CertificationCandidateExamSessionVO> updateCandidateSession(
      @PathVariable Long candidateId,
      @PathVariable Long sessionId,
      @Valid @RequestBody CertificationCandidateExamSessionSaveReq req) {
    return ApiResponse.ok(service.upsertCandidateSession(candidateId, sessionId, req));
  }

  @GetMapping("/exam-sessions/{sessionId}/candidates")
  @PreAuthorize("hasAuthority('certification.preparation.exam-session.view')")
  public ApiResponse<List<CertificationExamSessionCandidateVO>> listSessionCandidates(
      @PathVariable Long sessionId
  ) {
    return ApiResponse.ok(service.listSessionCandidates(sessionId));
  }

  @PutMapping("/exam-sessions/{sessionId}/candidates")
  @PreAuthorize("hasAuthority('certification.preparation.candidate.exam-session.edit')")
  @AuditLog(module = "certification", action = "candidate-exam-session:assign", targetType = "certification_candidate_exam_session")
  public ApiResponse<List<CertificationExamSessionCandidateVO>> assignSessionCandidates(
      @PathVariable Long sessionId,
      @Valid @RequestBody CertificationExamSessionCandidateAssignReq req
  ) {
    return ApiResponse.ok(service.assignSessionCandidates(sessionId, req));
  }

  @PostMapping("/exam-sessions/{sessionId}/collection-config/complete")
  @PreAuthorize("hasAuthority('certification.preparation.exam-session.edit')")
  @AuditLog(module = "certification", action = "exam-session:collection-config-complete", targetType = "certification_exam_session")
  public ApiResponse<CertificationExamSessionVO> completeCollectionConfig(@PathVariable Long sessionId) {
    return ApiResponse.ok(service.completeCollectionConfig(sessionId));
  }
}
