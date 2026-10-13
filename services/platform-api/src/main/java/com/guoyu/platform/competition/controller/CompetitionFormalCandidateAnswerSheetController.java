package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionFormalCandidateAnswerSheetExportReq;
import com.guoyu.platform.competition.service.CompetitionFormalCandidateAnswerSheetApplicationService;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateAnswerSheetFile;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateAnswerSheetVO;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/competition/formal-exam-candidates")
public class CompetitionFormalCandidateAnswerSheetController {

  private final CompetitionFormalCandidateAnswerSheetApplicationService service;

  public CompetitionFormalCandidateAnswerSheetController(
      CompetitionFormalCandidateAnswerSheetApplicationService service) {
    this.service = service;
  }

  @GetMapping("/{candidateExamId}/answer-sheet")
  @PreAuthorize("hasAuthority('competition:exam-session:view')")
  public ApiResponse<CompetitionFormalCandidateAnswerSheetVO> answerSheet(
      @PathVariable Long candidateExamId) {
    return ApiResponse.ok(service.getAnswerSheet(candidateExamId));
  }

  @GetMapping("/{candidateExamId}/answer-sheet/export")
  @PreAuthorize("hasAuthority('competition:exam-session:view')")
  @AuditLog(module = "competition", action = "formal-answer-sheet:export",
      targetType = "competition_formal_candidate_exam")
  public ResponseEntity<byte[]> exportAnswerSheet(@PathVariable Long candidateExamId) {
    return attachment(service.exportAnswerSheet(candidateExamId));
  }

  @PostMapping("/answer-sheets/export")
  @PreAuthorize("hasAuthority('competition:exam-session:view')")
  @AuditLog(module = "competition", action = "formal-answer-sheet:export-batch",
      targetType = "competition_formal_candidate_exam")
  public ResponseEntity<byte[]> exportAnswerSheets(
      @Valid @RequestBody CompetitionFormalCandidateAnswerSheetExportReq request) {
    return attachment(service.exportAnswerSheets(request.candidateExamIds()));
  }

  private static ResponseEntity<byte[]> attachment(CompetitionFormalCandidateAnswerSheetFile file) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.mediaType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
            .filename(file.filename(), StandardCharsets.UTF_8)
            .build()
            .toString())
        .body(file.data());
  }
}
