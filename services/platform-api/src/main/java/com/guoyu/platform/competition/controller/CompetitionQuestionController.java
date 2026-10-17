package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.web.DownloadHeaders;
import com.guoyu.platform.competition.dto.CompetitionQuestionCreateReq;
import com.guoyu.platform.competition.dto.CompetitionQuestionQueryReq;
import com.guoyu.platform.competition.dto.CompetitionQuestionUpdateReq;
import com.guoyu.platform.competition.dto.CompetitionQuestionBlueprintBindingReq;
import com.guoyu.platform.competition.dto.CompetitionQuestionBlueprintBatchBindingReq;
import com.guoyu.platform.competition.service.CompetitionQuestionApplicationService;
import com.guoyu.platform.competition.vo.CompetitionQuestionImportBatchVO;
import com.guoyu.platform.competition.vo.CompetitionQuestionVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/competition")
public class CompetitionQuestionController {

  private final CompetitionQuestionApplicationService service;

  public CompetitionQuestionController(CompetitionQuestionApplicationService service) {
    this.service = service;
  }

  @GetMapping("/events/{eventId}/questions")
  @PreAuthorize("hasAuthority('competition:question:view')")
  public ApiResponse<PageResponse<CompetitionQuestionVO>> page(
      @PathVariable Long eventId, @Valid @ModelAttribute CompetitionQuestionQueryReq req) {
    return ApiResponse.ok(service.page(eventId, req));
  }

  @GetMapping("/events/{eventId}/questions/{questionId}")
  @PreAuthorize("hasAuthority('competition:question:view')")
  public ApiResponse<CompetitionQuestionVO> get(
      @PathVariable Long eventId, @PathVariable Long questionId) {
    return ApiResponse.ok(service.get(eventId, questionId));
  }

  @PostMapping("/events/{eventId}/questions")
  @PreAuthorize("hasAuthority('competition:question:create')")
  @AuditLog(module = "competition", action = "question:create", targetType = "competition_question")
  public ApiResponse<CompetitionQuestionVO> create(
      @PathVariable Long eventId, @Valid @RequestBody CompetitionQuestionCreateReq req) {
    return ApiResponse.ok(service.create(eventId, req));
  }

  @PutMapping("/events/{eventId}/questions/{questionId}")
  @PreAuthorize("hasAuthority('competition:question:update')")
  @AuditLog(module = "competition", action = "question:update", targetType = "competition_question")
  public ApiResponse<CompetitionQuestionVO> update(
      @PathVariable Long eventId, @PathVariable Long questionId,
      @Valid @RequestBody CompetitionQuestionUpdateReq req) {
    return ApiResponse.ok(service.update(eventId, questionId, req));
  }

  @DeleteMapping("/events/{eventId}/questions/{questionId}")
  @PreAuthorize("hasAuthority('competition:question:delete')")
  @AuditLog(module = "competition", action = "question:delete", targetType = "competition_question")
  public ApiResponse<Void> delete(@PathVariable Long eventId, @PathVariable Long questionId) {
    service.delete(eventId, questionId);
    return ApiResponse.ok();
  }

  @PostMapping(value = "/events/{eventId}/question-imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('competition:question:import')")
  @AuditLog(module = "competition", action = "question:import", targetType = "competition_question_import_batch")
  public ApiResponse<CompetitionQuestionImportBatchVO> importFile(
      @PathVariable Long eventId,
      @RequestParam Long eventWorkTypeId,
      @RequestParam Long eventWorkTypeLevelId,
      @RequestParam String contentType,
      @RequestParam Long blueprintId,
      @RequestParam(required = false) String selectedSheet,
      @RequestPart("file") MultipartFile file) {
    return ApiResponse.ok(service.importFile(eventId, eventWorkTypeId, eventWorkTypeLevelId, contentType,
        blueprintId, selectedSheet, file));
  }

  @PostMapping(value = "/events/{eventId}/simple-question-imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('competition:question:import')")
  @AuditLog(module = "competition", action = "simple-question:import", targetType = "competition_question_import_batch")
  public ApiResponse<CompetitionQuestionImportBatchVO> importSimpleTheoryFile(
      @PathVariable Long eventId,
      @RequestParam Long eventWorkTypeId,
      @RequestParam Long eventWorkTypeLevelId,
      @RequestPart("file") MultipartFile file) {
    return ApiResponse.ok(service.importFile(eventId, eventWorkTypeId, eventWorkTypeLevelId, "theory", file));
  }

  @PostMapping(value = "/events/{eventId}/simple-practical-detail-imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('competition:question:import')")
  @AuditLog(module = "competition", action = "simple-practical-detail:import", targetType = "competition_question_import_batch")
  public ApiResponse<CompetitionQuestionImportBatchVO> importSimplePracticalDetails(
      @PathVariable Long eventId,
      @RequestParam Long eventWorkTypeId,
      @RequestParam Long eventWorkTypeLevelId,
      @RequestPart("file") MultipartFile file) {
    return ApiResponse.ok(service.importFile(eventId, eventWorkTypeId, eventWorkTypeLevelId, "practical", file));
  }

  @PostMapping("/events/{eventId}/simple-practical-questions")
  @PreAuthorize("hasAuthority('competition:question:create')")
  @AuditLog(module = "competition", action = "simple-practical:create", targetType = "competition_question")
  public ApiResponse<CompetitionQuestionVO> createSimplePracticalQuestion(
      @PathVariable Long eventId, @Valid @RequestBody CompetitionQuestionCreateReq req) {
    return ApiResponse.ok(service.createSimplePracticalQuestion(eventId, req));
  }

  @PutMapping("/events/{eventId}/questions/{questionId}/blueprint-binding")
  @PreAuthorize("hasAuthority('competition:question:update')")
  @AuditLog(module = "competition", action = "question:blueprint-bind", targetType = "competition_question")
  public ApiResponse<CompetitionQuestionVO> bindBlueprint(
      @PathVariable Long eventId, @PathVariable Long questionId,
      @Valid @RequestBody CompetitionQuestionBlueprintBindingReq req) {
    return ApiResponse.ok(service.bindBlueprint(eventId, questionId, req));
  }

  @PostMapping("/events/{eventId}/questions/blueprint-binding/batch")
  @PreAuthorize("hasAuthority('competition:question:update')")
  @AuditLog(module = "competition", action = "question:blueprint-bind-batch", targetType = "competition_question")
  public ApiResponse<List<CompetitionQuestionVO>> bindBlueprintBatch(
      @PathVariable Long eventId, @Valid @RequestBody CompetitionQuestionBlueprintBatchBindingReq req) {
    return ApiResponse.ok(service.bindBlueprintBatch(eventId, req));
  }

  @GetMapping("/events/{eventId}/question-imports/{batchId}")
  @PreAuthorize("hasAuthority('competition:question:view')")
  public ApiResponse<CompetitionQuestionImportBatchVO> getImportBatch(@PathVariable Long eventId, @PathVariable Long batchId) {
    return ApiResponse.ok(service.getImportBatch(eventId, batchId));
  }

  @GetMapping("/question-import-template")
  @PreAuthorize("hasAuthority('competition:question:view')")
  public ResponseEntity<byte[]> template(
      @RequestParam String contentType,
      @RequestParam(defaultValue = "false") boolean simple) {
    byte[] body = service.template(contentType, simple);
    String filename = simple
        ? "practical".equalsIgnoreCase(contentType)
            ? "competition-simple-practical-detail-template.xlsx"
            : "competition-simple-theory-question-template.xlsx"
        : "theory".equalsIgnoreCase(contentType)
            ? "competition-theory-question-template.xlsx" : "competition-practical-question-template.xlsx";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    headers.set(HttpHeaders.CONTENT_DISPOSITION, DownloadHeaders.contentDispositionAttachment(filename));
    return ResponseEntity.ok().headers(headers).body(body);
  }
}
