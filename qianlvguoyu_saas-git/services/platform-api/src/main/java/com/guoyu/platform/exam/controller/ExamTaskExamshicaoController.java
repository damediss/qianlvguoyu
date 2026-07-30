package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.exam.dto.ExamTaskExamshicaoAssignExpertReq;
import com.guoyu.platform.exam.dto.ExamTaskExamshicaoQueryReq;
import com.guoyu.platform.exam.service.ExamTaskExamshicaoApplicationService;
import com.guoyu.platform.exam.vo.ExamTaskExamshicaoDailyVO;
import com.guoyu.platform.exam.vo.ExamTaskExamshicaoDetailVO;
import com.guoyu.platform.exam.vo.ExamTaskExamshicaoVO;
import com.guoyu.platform.exam.vo.ExportResultDTO;
import com.guoyu.platform.exam.vo.PublicDataVO;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/exam")
@Slf4j
public class ExamTaskExamshicaoController {

  private final ExamTaskExamshicaoApplicationService service;

  public ExamTaskExamshicaoController(ExamTaskExamshicaoApplicationService service) {
    this.service = service;
  }

  @GetMapping("/task-examshicaos")
  @PreAuthorize("hasAuthority('task:examshicao-audit:view')")
  public ApiResponse<PageResponse<ExamTaskExamshicaoVO>> page(@Valid @ModelAttribute ExamTaskExamshicaoQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/task-examshicaos/{id}/details")
  @PreAuthorize("hasAuthority('task:examshicao-audit:view')")
  public ApiResponse<List<ExamTaskExamshicaoDetailVO>> details(@PathVariable Integer id) {
    return ApiResponse.ok(service.getDetails(id));
  }

  @GetMapping("/task-examshicaos/{id}/daily")
  @PreAuthorize("hasAuthority('task:examshicao-audit:view')")
  public ApiResponse<List<ExamTaskExamshicaoDailyVO>> daily(
      @PathVariable Integer id, @RequestParam Integer role) {
    return ApiResponse.ok(service.getDaily(id, role));
  }

  @PostMapping("/task-examshicaos/import")
  @PreAuthorize("hasAuthority('task:examshicao-audit:import')")
  @AuditLog(module = "exam", action = "task-examshicao-audit:import", targetType = "task_examshicao")
  public ApiResponse<Integer> importExcel(
      @RequestParam("file") MultipartFile file,
      @RequestParam Integer workTypeId,
      @RequestParam Integer level) {
    log.info("POST /exam/task-examshicaos/import workTypeId={}, level={}", workTypeId, level);
    int count = service.importExcel(file, workTypeId, level);
    return ApiResponse.ok(count);
  }

  @PostMapping("/task-examshicaos/{id}/assign-expert")
  @PreAuthorize("hasAuthority('task:examshicao-audit:assign-expert')")
  @AuditLog(module = "exam", action = "task-examshicao:assign-expert", targetType = "task_examshicao")
  public ApiResponse<Void> assignExpert(
      @PathVariable Integer id,
      @Valid @RequestBody ExamTaskExamshicaoAssignExpertReq req) {
    log.info("POST /exam/task-examshicaos/{}/assign-expert", id);
    service.assignExpert(req);
    return ApiResponse.ok();
  }

  @GetMapping("/task-examshicaos/{id}/export")
  @PreAuthorize("hasAuthority('task:examshicao-audit:export')")
  @AuditLog(module = "exam", action = "task-examshicao:export", targetType = "task_examshicao")
  public ResponseEntity<InputStreamResource> export(@PathVariable Integer id) throws IOException {
    log.info("GET /exam/task-examshicaos/{}/export", id);
    ExportResultDTO result = service.exportExcel(id);
    InputStreamResource resource = new InputStreamResource(new java.io.ByteArrayInputStream(result.data()));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(result.filename(), StandardCharsets.UTF_8).build().toString())
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(resource);
  }

  @DeleteMapping("/task-examshicaos/{id}")
  @PreAuthorize("hasAuthority('task:examshicao-audit:delete')")
  @AuditLog(module = "exam", action = "task-examshicao:delete", targetType = "task_examshicao")
  public ApiResponse<Void> delete(@PathVariable Integer id) {
    log.info("DELETE /exam/task-examshicaos/{}", id);
    service.delete(id);
    return ApiResponse.ok();
  }

  @GetMapping("/task-examshicaos/public-data")
  @PreAuthorize("hasAuthority('task:examshicao-audit:view')")
  public ApiResponse<PublicDataVO> publicData() {
    return ApiResponse.ok(service.getPublicData());
  }
}
