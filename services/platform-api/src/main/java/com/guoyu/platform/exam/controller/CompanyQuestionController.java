package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.web.DownloadHeaders;
import com.guoyu.platform.exam.dto.CompanyQuestionCreateReq;
import com.guoyu.platform.exam.dto.CompanyQuestionQueryReq;
import com.guoyu.platform.exam.dto.CompanyQuestionUpdateReq;
import com.guoyu.platform.exam.service.CompanyQuestionApplicationService;
import com.guoyu.platform.exam.vo.ExamExamDetailVO;
import com.guoyu.platform.exam.vo.QuestionImportResultVO;
import com.guoyu.platform.exam.vo.ExportResultDTO;
import jakarta.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/exam")
@Slf4j
public class CompanyQuestionController {

  private final CompanyQuestionApplicationService service;

  public CompanyQuestionController(CompanyQuestionApplicationService service) {
    this.service = service;
  }

  @GetMapping("/company-questions")
  @PreAuthorize("hasAuthority('exam:question:view')")
  public ApiResponse<PageResponse<ExamExamDetailVO>> page(
      @Valid @ModelAttribute CompanyQuestionQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/company-questions/{id}")
  @PreAuthorize("hasAuthority('exam:question:view')")
  public ApiResponse<ExamExamDetailVO> get(@PathVariable Integer id) {
    return ApiResponse.ok(service.getById(id));
  }

  @PostMapping("/company-questions")
  @PreAuthorize("hasAuthority('exam:question:create')")
  @AuditLog(module = "exam", action = "companyQuestion:create", targetType = "exam_exam_detail")
  public ApiResponse<ExamExamDetailVO> create(
      @Valid @RequestBody CompanyQuestionCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  @PutMapping("/company-questions/{id}")
  @PreAuthorize("hasAuthority('exam:question:update')")
  @AuditLog(module = "exam", action = "companyQuestion:update", targetType = "exam_exam_detail")
  public ApiResponse<ExamExamDetailVO> update(
      @PathVariable Integer id,
      @Valid @RequestBody CompanyQuestionUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  @DeleteMapping("/company-questions/{id}")
  @PreAuthorize("hasAuthority('exam:question:delete')")
  @AuditLog(module = "exam", action = "companyQuestion:delete", targetType = "exam_exam_detail")
  public ApiResponse<Void> delete(@PathVariable Integer id) {
    service.delete(id);
    return ApiResponse.ok();
  }

  @PostMapping("/company-questions/batch-delete")
  @PreAuthorize("hasAuthority('exam:question:delete')")
  @AuditLog(module = "exam", action = "companyQuestion:batchDelete", targetType = "exam_exam_detail")
  public ApiResponse<Integer> batchDelete(@RequestBody List<Integer> ids) {
    int n = service.batchDelete(ids);
    return ApiResponse.ok(n);
  }

  @PostMapping("/company-questions/import")
  @PreAuthorize("hasAuthority('exam:question:create')")
  @AuditLog(module = "exam", action = "companyQuestion:import", targetType = "exam_exam_detail")
  public ApiResponse<QuestionImportResultVO> importExcel(
      @RequestParam("file") MultipartFile file,
      @RequestParam Long orgId,
      @RequestParam String orgName,
      @RequestParam(required = false, defaultValue = "0") Integer source) {
    log.info("POST /exam/company-questions/import orgId={}, source={}", orgId, source);
    return ApiResponse.ok(service.importQuestions(file, orgId, orgName, source));
  }

  @GetMapping("/company-questions/export")
  @PreAuthorize("hasAuthority('exam:question:view')")
  @AuditLog(module = "exam", action = "companyQuestion:export", targetType = "exam_exam_detail")
  public ResponseEntity<InputStreamResource> exportByOrg(
      @RequestParam(required = false) Long orgId) throws IOException {
    log.info("GET /exam/company-questions/export orgId={}", orgId);
    ExportResultDTO result = service.exportByOrg(orgId);
    InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(result.data()));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            DownloadHeaders.contentDispositionAttachment(result.filename()))
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(resource);
  }
}
