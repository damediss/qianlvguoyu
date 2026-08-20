package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.trainingdepartment.dto.ExamScoreImportCommitReq;
import com.guoyu.platform.trainingdepartment.security.TrainingAuthorityExpressions;
import com.guoyu.platform.trainingdepartment.service.ExamScoreImportApplicationService;
import com.guoyu.platform.trainingdepartment.vo.ExamScoreImportBatchItemVO;
import com.guoyu.platform.trainingdepartment.vo.ExamScoreImportBatchVO;
import com.guoyu.platform.trainingdepartment.vo.ExamScoreImportClassVO;
import com.guoyu.platform.trainingdepartment.vo.ExamScoreImportCommitResultVO;
import com.guoyu.platform.trainingdepartment.vo.ExamScoreImportPreviewVO;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/training/exam-score-imports")
public class ExamScoreImportController {

  private final ExamScoreImportApplicationService service;

  public ExamScoreImportController(ExamScoreImportApplicationService service) {
    this.service = service;
  }

  @GetMapping("/template")
  @PreAuthorize(TrainingAuthorityExpressions.EXAM_SCORE_IMPORT_VIEW)
  public ResponseEntity<byte[]> template() {
    return service.downloadTemplate();
  }

  @GetMapping("/classes")
  @PreAuthorize(TrainingAuthorityExpressions.EXAM_SCORE_IMPORT_VIEW)
  public ApiResponse<List<ExamScoreImportClassVO>> classes(
      @RequestParam(required = false) Long companyId,
      @RequestParam(required = false) Long trainInfoId,
      @RequestParam(required = false) String keyword) {
    return ApiResponse.ok(service.listClasses(companyId, trainInfoId, keyword));
  }

  @PostMapping("/preview")
  @PreAuthorize(TrainingAuthorityExpressions.EXAM_SCORE_IMPORT_UPDATE)
  public ApiResponse<ExamScoreImportPreviewVO> preview(
      @RequestParam(value = "classIds", required = false) List<String> classIds,
      @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(service.preview(parseClassIds(classIds), file));
  }

  @PostMapping("/commit")
  @PreAuthorize(TrainingAuthorityExpressions.EXAM_SCORE_IMPORT_UPDATE)
  @AuditLog(module = "training", action = "exam-score-import:commit", targetType = "train_exam_score_import_batch")
  public ApiResponse<ExamScoreImportCommitResultVO> commit(@Valid @RequestBody ExamScoreImportCommitReq req) {
    return ApiResponse.ok(service.commit(req));
  }

  @GetMapping("/batches")
  @PreAuthorize(TrainingAuthorityExpressions.EXAM_SCORE_IMPORT_VIEW)
  public ApiResponse<PageResponse<ExamScoreImportBatchVO>> batches(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long pageSize) {
    return ApiResponse.ok(service.pageBatches(page, pageSize));
  }

  @GetMapping("/batches/{id}/items")
  @PreAuthorize(TrainingAuthorityExpressions.EXAM_SCORE_IMPORT_VIEW)
  public ApiResponse<PageResponse<ExamScoreImportBatchItemVO>> items(
      @PathVariable Long id,
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "50") long pageSize) {
    return ApiResponse.ok(service.pageItems(id, page, pageSize));
  }

  @GetMapping("/batches/{id}/failures.xlsx")
  @PreAuthorize(TrainingAuthorityExpressions.EXAM_SCORE_IMPORT_VIEW)
  public ResponseEntity<byte[]> exportFailures(@PathVariable Long id) {
    return service.exportFailures(id);
  }

  static List<Long> parseClassIds(List<String> raw) {
    if (raw == null || raw.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先选择班级后再上传成绩单");
    }
    List<Long> ids = new ArrayList<>();
    for (String part : raw) {
      if (!StringUtils.hasText(part)) {
        continue;
      }
      for (String token : part.split("[,，\\s]+")) {
        if (!StringUtils.hasText(token)) {
          continue;
        }
        try {
          ids.add(Long.parseLong(token.trim()));
        } catch (NumberFormatException ex) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "班级参数不合法");
        }
      }
    }
    return ids;
  }
}
