package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionQuestionBlueprintQueryReq;
import com.guoyu.platform.competition.service.CompetitionQuestionBlueprintApplicationService;
import com.guoyu.platform.competition.vo.CompetitionQuestionBlueprintTreeVO;
import com.guoyu.platform.competition.vo.CompetitionQuestionBlueprintVO;
import com.guoyu.platform.competition.vo.CompetitionQuestionBlueprintCoverageVO;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/competition/events/{eventId}/question-blueprints")
public class CompetitionQuestionBlueprintController {

  private final CompetitionQuestionBlueprintApplicationService service;

  public CompetitionQuestionBlueprintController(CompetitionQuestionBlueprintApplicationService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('competition:question-blueprint:view')")
  public ApiResponse<PageResponse<CompetitionQuestionBlueprintVO>> page(
      @PathVariable Long eventId, @Valid @ModelAttribute CompetitionQuestionBlueprintQueryReq req) {
    return ApiResponse.ok(service.page(eventId, req));
  }

  @GetMapping("/{blueprintId}/tree")
  @PreAuthorize("hasAuthority('competition:question-blueprint:view')")
  public ApiResponse<CompetitionQuestionBlueprintTreeVO> tree(
      @PathVariable Long eventId, @PathVariable Long blueprintId) {
    service.findForEvent(com.guoyu.platform.common.tenant.TenantContextHolder.getTenantId(), eventId, blueprintId);
    return ApiResponse.ok(service.tree(blueprintId));
  }

  @GetMapping("/{blueprintId}/coverage")
  @PreAuthorize("hasAuthority('competition:question-blueprint:view')")
  public ApiResponse<CompetitionQuestionBlueprintCoverageVO> coverage(
      @PathVariable Long eventId, @PathVariable Long blueprintId) {
    service.findForEvent(com.guoyu.platform.common.tenant.TenantContextHolder.getTenantId(), eventId, blueprintId);
    return ApiResponse.ok(service.coverage(blueprintId));
  }

  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('competition:question-blueprint:import')")
  @AuditLog(module = "competition", action = "question-blueprint:import", targetType = "competition_question_blueprint")
  public ApiResponse<CompetitionQuestionBlueprintVO> importFile(
      @PathVariable Long eventId,
      @RequestParam Long eventWorkTypeId,
      @RequestParam Long eventWorkTypeLevelId,
      @RequestParam String contentType,
      @RequestParam(defaultValue = "false") boolean confirmWarnings,
      @RequestPart("file") MultipartFile file) {
    return ApiResponse.ok(service.importBlueprint(eventId, eventWorkTypeId, eventWorkTypeLevelId,
        contentType, file, confirmWarnings));
  }

  @PostMapping("/{blueprintId}/activate")
  @PreAuthorize("hasAuthority('competition:question-blueprint:activate')")
  @AuditLog(module = "competition", action = "question-blueprint:activate", targetType = "competition_question_blueprint")
  public ApiResponse<CompetitionQuestionBlueprintVO> activate(
      @PathVariable Long eventId, @PathVariable Long blueprintId,
      @RequestParam(defaultValue = "false") boolean confirmWarnings) {
    service.findForEvent(com.guoyu.platform.common.tenant.TenantContextHolder.getTenantId(), eventId, blueprintId);
    return ApiResponse.ok(service.activate(blueprintId, confirmWarnings));
  }

  @DeleteMapping("/{blueprintId}")
  @PreAuthorize("hasAuthority('competition:question-blueprint:archive')")
  @AuditLog(module = "competition", action = "question-blueprint:archive", targetType = "competition_question_blueprint")
  public ApiResponse<Void> archive(@PathVariable Long eventId, @PathVariable Long blueprintId) {
    service.findForEvent(com.guoyu.platform.common.tenant.TenantContextHolder.getTenantId(), eventId, blueprintId);
    service.archive(blueprintId);
    return ApiResponse.ok();
  }

  @GetMapping("/{blueprintId}/source-file")
  @PreAuthorize("hasAuthority('competition:question-blueprint:view')")
  public ResponseEntity<byte[]> sourceFile(@PathVariable Long eventId, @PathVariable Long blueprintId) {
    var bp = service.findForEvent(com.guoyu.platform.common.tenant.TenantContextHolder.getTenantId(), eventId, blueprintId);
    if (bp.getSourceFileId() == null) throw new com.guoyu.platform.common.exception.BusinessException(
        com.guoyu.platform.common.exception.ErrorCode.NOT_FOUND, "细目表源文件不存在");
    byte[] bytes = service.readSourceFile(bp.getSourceFileId());
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    headers.setContentDisposition(ContentDisposition.attachment().filename(bp.getSourceFileName(), StandardCharsets.UTF_8).build());
    return ResponseEntity.ok().headers(headers).body(bytes);
  }
}
