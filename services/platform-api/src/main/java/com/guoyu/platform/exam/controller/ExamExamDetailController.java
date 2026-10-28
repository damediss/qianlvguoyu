package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.ExamExamDetailCreateReq;
import com.guoyu.platform.exam.dto.ExamExamDetailQueryReq;
import com.guoyu.platform.exam.dto.ExamExamDetailUpdateReq;
import com.guoyu.platform.exam.service.ExamExamDetailApplicationService;
import com.guoyu.platform.exam.vo.ExamExamDetailVO;
import com.guoyu.platform.exam.vo.QuestionImportResultVO;
import com.guoyu.platform.exam.vo.QuestionImportValidateVO;
import com.guoyu.platform.exam.vo.ExportResultDTO;
import jakarta.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  /**
   * 导入前只读预检：不写库、不调用 AI。属既有导入链，沿用 exam:question:create，
   * 不套用 AI 工作台的 PLATFORM_ADMIN 限制（方案 §14.4.3）。
   */
  @PostMapping("/exam-details/import/validate")
  @PreAuthorize("hasAuthority('exam:question:create')")
  public ApiResponse<QuestionImportValidateVO> validateImport(
      @RequestParam("file") MultipartFile file,
      @RequestParam Integer courseId) {
    log.info("POST /exam/exam-details/import/validate courseId={}", courseId);
    return ApiResponse.ok(service.validateExamDetailsImport(file, courseId));
  }

  @PostMapping("/exam-details/import")
  @PreAuthorize("hasAuthority('exam:question:create')")
  @AuditLog(module = "exam", action = "question:import", targetType = "exam_exam_detail")
  public ApiResponse<QuestionImportResultVO> importExcel(
      @RequestParam("file") MultipartFile file,
      @RequestParam Integer courseId,
      @RequestParam(required = false, defaultValue = "0") Integer source,
      @RequestParam(required = false) String validationFileSha256) {
    log.info("POST /exam/exam-details/import courseId={}, source={}", courseId, source);
    return ApiResponse.ok(
        service.importExamDetails(file, courseId, source, validationFileSha256));
  }

  @GetMapping("/exam-details/export")
  @PreAuthorize("hasAuthority('exam:question:view')")
  @AuditLog(module = "exam", action = "question:export", targetType = "exam_exam_detail")
  public ResponseEntity<InputStreamResource> exportByCourse(
      @RequestParam Integer courseId,
      @RequestParam(required = false) List<Integer> ids,
      @RequestParam(required = false) Integer sampleSingle,
      @RequestParam(required = false) Integer sampleMulti,
      @RequestParam(required = false) Integer sampleJudge,
      @RequestParam(required = false, defaultValue = "0") Integer sampleSource) throws IOException {
    boolean sampledRequested = sampleSingle != null || sampleMulti != null || sampleJudge != null;
    ExportResultDTO result;
    String sampledCounts = null;
    if (sampledRequested) {
      if (ids != null && !ids.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "勾选导出与配额抽题导出不能同时使用");
      }
      if ((sampleSingle != null && sampleSingle < 0) || (sampleMulti != null && sampleMulti < 0)
          || (sampleJudge != null && sampleJudge < 0)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "抽题数量不能为负数");
      }
      Map<String, Integer> quotas = new LinkedHashMap<>();
      quotas.put("single", sampleSingle);
      quotas.put("multi", sampleMulti);
      quotas.put("judge", sampleJudge);
      log.info("GET /exam/exam-details/export courseId={}, sample quotas={}, source={}",
          courseId, quotas, sampleSource);
      ExamExamDetailApplicationService.SampledExport sampled =
          service.exportSampledByQuotas(courseId, quotas, sampleSource);
      result = sampled.result();
      sampledCounts = sampled.sampledCounts();
    } else {
      log.info("GET /exam/exam-details/export courseId={}, selected={}", courseId,
          ids == null ? 0 : ids.size());
      result = service.exportByCourse(courseId, ids);
    }
    InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(result.data()));
    // 使用 RFC 5987 filename*，避免 Spring ContentDisposition 生成 =?UTF-8?Q?...?=
    // 导致前端无法正确解析中文文件名
    String encodedName = URLEncoder.encode(result.filename(), StandardCharsets.UTF_8).replace("+", "%20");
    ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    if (sampledCounts != null) {
      // 实际抽到数量回传前端，用于提示"某题型仅抽到 X/配额"
      builder.header("X-Export-Sampled", sampledCounts);
    }
    return builder.body(resource);
  }
}
