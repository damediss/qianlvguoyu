package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.dto.PreparationCandidateImportReq;
import com.guoyu.platform.certification.dto.PreparationCandidateQueryReq;
import com.guoyu.platform.certification.dto.PreparationCandidateSaveReq;
import com.guoyu.platform.certification.service.CertificationPreparationApplicationService;
import com.guoyu.platform.certification.service.CertificationCandidateCsvImportService;
import com.guoyu.platform.certification.vo.PreparationCandidateVO;
import com.guoyu.platform.certification.vo.PreparationCandidateStagingVO;
import com.guoyu.platform.certification.vo.PreparationImportBatchVO;
import com.guoyu.platform.certification.vo.PreparationImportErrorVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageQuery;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import jakarta.validation.Valid;
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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Validated
@RestController
@RequestMapping("/certification/preparation")
public class CertificationPreparationCandidateController {

  private final CertificationPreparationApplicationService service;
  private final CertificationCandidateCsvImportService excelImportService;

  public CertificationPreparationCandidateController(
      CertificationPreparationApplicationService service,
      CertificationCandidateCsvImportService excelImportService
  ) {
    this.service = service;
    this.excelImportService = excelImportService;
  }

  @GetMapping(value = "/candidate-import-template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  @PreAuthorize("hasAuthority('certification.preparation.candidate.import')")
  public ResponseEntity<byte[]> downloadExcelTemplate() {
    // RFC 5987 filename* 必须百分号编码；裸中文会触发 Tomcat 写响应头失败 → 500
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
            .filename("考生信息批量导入模板.xlsx", StandardCharsets.UTF_8).build().toString())
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(excelImportService.templateBytes());
  }

  @PostMapping(value = "/tasks/{taskId}/candidate-imports/xlsx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('certification.preparation.candidate.import')")
  @AuditLog(module = "certification", action = "preparation-candidate:xlsx-import", targetType = "certification_candidate")
  public ApiResponse<PreparationImportBatchVO> importExcel(
      @PathVariable Long taskId,
      @RequestParam Long taskScopeId,
      @RequestPart MultipartFile file,
      @RequestParam(required = false) String remark,
      @RequestParam(required = false) List<Long> examSessionIds,
      @RequestParam(required = false) String defaultParticipation
  ) {
    return ApiResponse.ok(excelImportService.importExcel(
        taskId, taskScopeId, file, remark, examSessionIds, defaultParticipation));
  }

  @PostMapping("/tasks/{taskId}/candidate-imports/{batchId}/application-materials")
  @PreAuthorize("hasAuthority('certification.preparation.material.generate')")
  @AuditLog(module = "certification", action = "preparation-material:generate-import-batch", targetType = "certification_application_material_batch")
  public ApiResponse<com.guoyu.platform.certification.vo.PreparationMaterialBatchVO> generateImportBatchMaterials(
      @PathVariable Long taskId,
      @PathVariable Long batchId,
      @RequestParam(required = false) String remark
  ) {
    return ApiResponse.ok(service.generateApplicationMaterialsForImportBatch(taskId, batchId, remark));
  }

  @PostMapping("/tasks/{taskId}/candidate-imports")
  @PreAuthorize("hasAuthority('certification.preparation.candidate.import')")
  @AuditLog(module = "certification", action = "preparation-candidate:import", targetType = "certification_candidate")
  public ApiResponse<PreparationImportBatchVO> createImportBatch(
      @PathVariable Long taskId,
      @Valid @RequestBody PreparationCandidateImportReq req
  ) {
    return ApiResponse.ok(service.createImportBatch(taskId, req));
  }

  @GetMapping("/tasks/{taskId}/candidate-imports/{batchId}/errors")
  @PreAuthorize("hasAuthority('certification.preparation.candidate.view')")
  public ApiResponse<PageResponse<PreparationImportErrorVO>> listImportErrors(
      @PathVariable Long taskId,
      @PathVariable Long batchId,
      @Valid @ModelAttribute PageQuery req
  ) {
    return ApiResponse.ok(service.pageImportErrors(taskId, batchId, req));
  }

  @GetMapping("/tasks/{taskId}/candidates")
  @PreAuthorize("hasAuthority('certification.preparation.candidate.view')")
  public ApiResponse<PageResponse<PreparationCandidateVO>> pageCandidates(
      @PathVariable Long taskId,
      @Valid @ModelAttribute PreparationCandidateQueryReq req
  ) {
    return ApiResponse.ok(service.pageCandidates(taskId, req));
  }

  @PostMapping("/tasks/{taskId}/candidates")
  @PreAuthorize("hasAuthority('certification.preparation.candidate.edit')")
  @AuditLog(module = "certification", action = "preparation-candidate:create", targetType = "certification_candidate")
  public ApiResponse<PreparationCandidateVO> createCandidate(
      @PathVariable Long taskId,
      @Valid @RequestBody PreparationCandidateSaveReq req
  ) {
    return ApiResponse.ok(service.createCandidate(taskId, req));
  }

  @GetMapping("/tasks/{taskId}/candidates/{candidateId}/application-source")
  @PreAuthorize("hasAuthority('certification.preparation.candidate.view')")
  public ApiResponse<PreparationCandidateStagingVO> getCandidateApplicationSource(
      @PathVariable Long taskId,
      @PathVariable Long candidateId
  ) {
    return ApiResponse.ok(service.getCandidateApplicationSource(taskId, candidateId));
  }

  @PutMapping("/tasks/{taskId}/candidates/{candidateId}")
  @PreAuthorize("hasAuthority('certification.preparation.candidate.edit')")
  @AuditLog(module = "certification", action = "preparation-candidate:update", targetType = "certification_candidate")
  public ApiResponse<PreparationCandidateVO> updateCandidate(
      @PathVariable Long taskId,
      @PathVariable Long candidateId,
      @Valid @RequestBody PreparationCandidateSaveReq req
  ) {
    return ApiResponse.ok(service.updateCandidate(taskId, candidateId, req));
  }

  @DeleteMapping("/tasks/{taskId}/candidates/{candidateId}")
  @PreAuthorize("hasAuthority('certification.preparation.candidate.delete')")
  @AuditLog(module = "certification", action = "preparation-candidate:delete", targetType = "certification_candidate")
  public ApiResponse<Void> deleteCandidate(@PathVariable Long taskId, @PathVariable Long candidateId) {
    service.deleteCandidate(taskId, candidateId);
    return ApiResponse.ok();
  }
}
