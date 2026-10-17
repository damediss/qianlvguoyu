package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.web.DownloadHeaders;
import com.guoyu.platform.competition.dto.CompetitionSimulationImportConfirmReq;
import com.guoyu.platform.competition.service.CompetitionSubjectScoreImportService;
import com.guoyu.platform.competition.vo.CompetitionSubjectScoreImportBatchVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequestMapping("/competition/subject-score-imports")
public class CompetitionSubjectScoreImportController {
  private final CompetitionSubjectScoreImportService service;

  public CompetitionSubjectScoreImportController(CompetitionSubjectScoreImportService service) {
    this.service = service;
  }

  @GetMapping("/template")
  @PreAuthorize("hasAuthority('competition:score:import')")
  public ResponseEntity<byte[]> template(@RequestParam @NotBlank String sourceType) {
    String fileName = "practical".equalsIgnoreCase(sourceType)
        ? "实操成绩导入模板.xlsx" : "仿真成绩导入模板.xlsx";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    headers.set(HttpHeaders.CONTENT_DISPOSITION,
        DownloadHeaders.contentDispositionAttachment(fileName));
    return new ResponseEntity<>(service.template(sourceType), headers, HttpStatus.OK);
  }

  @PostMapping
  @PreAuthorize("hasAuthority('competition:score:import')")
  @AuditLog(module = "competition", action = "subject-score:upload",
      targetType = "competition_subject_score_import_batch", includeResult = true)
  public ResponseEntity<ApiResponse<CompetitionSubjectScoreImportBatchVO>> upload(
      @RequestParam @NotNull Long eventId,
      @RequestParam @NotNull Long stageId,
      @RequestParam @NotNull Long eventWorkTypeId,
      @RequestParam(required = false) Long eventWorkTypeLevelId,
      @RequestParam @NotBlank String sourceType,
      @RequestParam MultipartFile file) throws Exception {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CompetitionSubjectScoreImportBatchVO batch = CompetitionSubjectScoreImportBatchVO.from(
        service.upload(user.getTenantId(), user.getUserId(), eventId, stageId, eventWorkTypeId,
            eventWorkTypeLevelId, sourceType,
            file == null ? null : file.getOriginalFilename(),
            file == null ? null : file.getBytes()));
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(batch));
  }

  @GetMapping("/{batchId}")
  @PreAuthorize("hasAuthority('competition:score:view')")
  public ApiResponse<CompetitionSubjectScoreImportBatchVO> detail(@PathVariable Long batchId) {
    return ApiResponse.ok(CompetitionSubjectScoreImportBatchVO.from(
        service.detail(SecurityContext.currentUser().getTenantId(), batchId)));
  }

  @GetMapping("/{batchId}/errors")
  @PreAuthorize("hasAuthority('competition:score:view')")
  public ResponseEntity<byte[]> errors(@PathVariable Long batchId) {
    byte[] content = service.errorWorkbook(SecurityContext.currentUser().getTenantId(), batchId);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    headers.set(HttpHeaders.CONTENT_DISPOSITION,
        DownloadHeaders.contentDispositionAttachment("成绩导入错误明细.xlsx"));
    return new ResponseEntity<>(content, headers, HttpStatus.OK);
  }

  @PostMapping("/{batchId}/confirm")
  @PreAuthorize("hasAuthority('competition:score:import')")
  @AuditLog(module = "competition", action = "subject-score:confirm",
      targetType = "competition_subject_score_import_batch", includeResult = true)
  public ResponseEntity<ApiResponse<CompetitionSubjectScoreImportBatchVO>> confirm(
      @PathVariable Long batchId,
      @Valid @RequestBody(required = false) CompetitionSimulationImportConfirmReq request) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CompetitionSubjectScoreImportBatchVO batch = CompetitionSubjectScoreImportBatchVO.from(
        service.confirm(user.getTenantId(), user.getUserId(), batchId,
            request == null ? null : request.correctionReason()));
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(batch));
  }
}
