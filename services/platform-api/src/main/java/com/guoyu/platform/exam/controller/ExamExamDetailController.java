package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.exam.dto.ExamExamDetailCreateReq;
import com.guoyu.platform.exam.dto.ExamExamDetailQueryReq;
import com.guoyu.platform.exam.dto.ExamExamDetailUpdateReq;
import com.guoyu.platform.exam.service.ExamExamDetailApplicationService;
import com.guoyu.platform.exam.vo.ExamExamDetailVO;
import com.guoyu.platform.exam.vo.ExportResultDTO;
import jakarta.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
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
public class ExamExamDetailController {

  private final ExamExamDetailApplicationService service;

  public ExamExamDetailController(ExamExamDetailApplicationService service) {
    this.service = service;
  }

  @GetMapping("/exam-details")
  @PreAuthorize("hasAuthority('exam:question:view')")
  public ApiResponse<PageResponse<ExamExamDetailVO>> page(
      @Valid @ModelAttribute ExamExamDetailQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/exam-details/{id}")
  @PreAuthorize("hasAuthority('exam:question:view')")
  public ApiResponse<ExamExamDetailVO> get(@PathVariable Integer id) {
    return ApiResponse.ok(service.getById(id));
  }

  @PostMapping("/exam-details")
  @PreAuthorize("hasAuthority('exam:question:create')")
  @AuditLog(module = "exam", action = "question:create", targetType = "exam_exam_detail")
  public ApiResponse<ExamExamDetailVO> create(
      @Valid @RequestBody ExamExamDetailCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  @PutMapping("/exam-details/{id}")
  @PreAuthorize("hasAuthority('exam:question:update')")
  @AuditLog(module = "exam", action = "question:update", targetType = "exam_exam_detail")
  public ApiResponse<ExamExamDetailVO> update(
      @PathVariable Integer id,
      @Valid @RequestBody ExamExamDetailUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  @DeleteMapping("/exam-details/{id}")
  @PreAuthorize("hasAuthority('exam:question:delete')")
  @AuditLog(module = "exam", action = "question:delete", targetType = "exam_exam_detail")
  public ApiResponse<Void> delete(@PathVariable Integer id) {
    service.delete(id);
    return ApiResponse.ok();
  }

  @PostMapping("/exam-details/batch-delete")
  @PreAuthorize("hasAuthority('exam:question:delete')")
  @AuditLog(module = "exam", action = "question:batchDelete", targetType = "exam_exam_detail")
  public ApiResponse<Integer> batchDelete(@RequestBody List<Integer> ids) {
    int n = service.batchDelete(ids);
    return ApiResponse.ok(n);
  }

  @PostMapping("/exam-details/import")
  @PreAuthorize("hasAuthority('exam:question:create')")
  @AuditLog(module = "exam", action = "question:import", targetType = "exam_exam_detail")
  public ApiResponse<Integer> importExcel(
      @RequestParam("file") MultipartFile file,
      @RequestParam Integer courseId,
      @RequestParam(required = false, defaultValue = "0") Integer source) {
    log.info("POST /exam/exam-details/import courseId={}, source={}", courseId, source);
    int count = service.importExamDetails(file, courseId, source);
    return ApiResponse.ok(count);
  }

  @GetMapping("/exam-details/export")
  @PreAuthorize("hasAuthority('exam:question:view')")
  @AuditLog(module = "exam", action = "question:export", targetType = "exam_exam_detail")
  public ResponseEntity<InputStreamResource> exportByCourse(
      @RequestParam Integer courseId) throws IOException {
    log.info("GET /exam/exam-details/export courseId={}", courseId);
    ExportResultDTO result = service.exportByCourse(courseId);
    InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(result.data()));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(result.filename(), StandardCharsets.UTF_8).build().toString())
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(resource);
  }
}
