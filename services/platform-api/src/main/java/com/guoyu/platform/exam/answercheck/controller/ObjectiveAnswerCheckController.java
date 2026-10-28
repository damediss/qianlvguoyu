package com.guoyu.platform.exam.answercheck.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.answercheck.dto.ObjectiveAnswerItemFilter;
import com.guoyu.platform.exam.answercheck.dto.ObjectiveAnswerQuestionPageReq;
import com.guoyu.platform.exam.answercheck.dto.ObjectiveAnswerSelectionReq;
import com.guoyu.platform.exam.answercheck.service.ObjectiveAnswerCheckApplicationService;
import com.guoyu.platform.exam.answercheck.vo.ObjectiveAnswerBatchCreatedVO;
import com.guoyu.platform.exam.answercheck.vo.ObjectiveAnswerBatchVO;
import com.guoyu.platform.exam.answercheck.vo.ObjectiveAnswerFilterOptionsVO;
import com.guoyu.platform.exam.answercheck.vo.ObjectiveAnswerItemDetailVO;
import com.guoyu.platform.exam.answercheck.vo.ObjectiveAnswerItemVO;
import com.guoyu.platform.exam.answercheck.vo.ObjectiveAnswerPreviewVO;
import com.guoyu.platform.exam.answercheck.vo.ObjectiveAnswerQuestionPageVO;
import com.guoyu.platform.exam.answercheck.vo.ObjectiveAnswerReviewReq;
import com.guoyu.platform.exam.vo.ExportResultDTO;
import com.guoyu.platform.security.PlatformAdminSupport;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客观题答案 AI 检查 HTTP 入口（方案 §13）。
 *
 * <p>除权限码外，所有入口都必须是平台管理员：一期数据范围固定为“当前租户全部存活题库”，
 * 不能只靠分配权限码扩大范围。前端隐藏按钮不能替代这里。
 */
@Slf4j
@RestController
@RequestMapping("/exam/objective-answer-check")
public class ObjectiveAnswerCheckController {

  private final ObjectiveAnswerCheckApplicationService service;

  public ObjectiveAnswerCheckController(ObjectiveAnswerCheckApplicationService service) {
    this.service = service;
  }

  @GetMapping("/filter-options")
  @PreAuthorize("hasAuthority('exam:objective-answer-check:view')")
  public ApiResponse<ObjectiveAnswerFilterOptionsVO> filterOptions(
      @RequestParam(required = false) Integer workTypeId) {
    requirePlatformAdmin();
    return ApiResponse.ok(service.filterOptions(workTypeId));
  }

  @PostMapping("/questions/page")
  @PreAuthorize("hasAuthority('exam:objective-answer-check:view')")
  public ApiResponse<ObjectiveAnswerQuestionPageVO> questions(
      @Valid @RequestBody ObjectiveAnswerQuestionPageReq req) {
    requirePlatformAdmin();
    return ApiResponse.ok(service.pageQuestions(req.scope(),
        req.page() == null ? 1 : req.page(), req.pageSize() == null ? 20 : req.pageSize()));
  }

  @PostMapping("/selection/preview")
  @PreAuthorize("hasAuthority('exam:objective-answer-check:view')")
  public ApiResponse<ObjectiveAnswerPreviewVO> preview(
      @Valid @RequestBody ObjectiveAnswerSelectionReq req) {
    requirePlatformAdmin();
    return ApiResponse.ok(service.preview(req));
  }

  @PostMapping("/batches")
  @PreAuthorize("hasAuthority('exam:objective-answer-check:submit')")
  @AuditLog(module = "exam", action = "objectiveAnswerCheck:create",
      targetType = "exam_objective_answer_check_batch")
  public ApiResponse<ObjectiveAnswerBatchCreatedVO> create(
      @Valid @RequestBody ObjectiveAnswerSelectionReq req) {
    PlatformUserPrincipal principal = requirePlatformAdmin();
    Long batchId = service.create(req, principal.getTenantId(), principal.getUserId());
    return ApiResponse.ok(new ObjectiveAnswerBatchCreatedVO(batchId));
  }

  @GetMapping("/batches/current")
  @PreAuthorize("hasAuthority('exam:objective-answer-check:view')")
  public ApiResponse<ObjectiveAnswerBatchVO> current() {
    PlatformUserPrincipal principal = requirePlatformAdmin();
    return ApiResponse.ok(service.current(principal.getTenantId()));
  }

  @GetMapping("/batches")
  @PreAuthorize("hasAuthority('exam:objective-answer-check:view')")
  public ApiResponse<PageResponse<ObjectiveAnswerBatchVO>> batches(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "10") long pageSize) {
    requirePlatformAdmin();
    return ApiResponse.ok(service.pageBatches(page, pageSize));
  }

  @GetMapping("/batches/{batchId}")
  @PreAuthorize("hasAuthority('exam:objective-answer-check:view')")
  public ApiResponse<ObjectiveAnswerBatchVO> batch(@PathVariable Long batchId) {
    requirePlatformAdmin();
    return ApiResponse.ok(service.getBatch(batchId));
  }

  @PostMapping("/batches/{batchId}/pause")
  @PreAuthorize("hasAuthority('exam:objective-answer-check:submit')")
  @AuditLog(module = "exam", action = "objectiveAnswerCheck:pause",
      targetType = "exam_objective_answer_check_batch")
  public ApiResponse<Void> pause(@PathVariable Long batchId) {
    requirePlatformAdmin();
    service.pause(batchId);
    return ApiResponse.ok();
  }

  @PostMapping("/batches/{batchId}/resume")
  @PreAuthorize("hasAuthority('exam:objective-answer-check:submit')")
  @AuditLog(module = "exam", action = "objectiveAnswerCheck:resume",
      targetType = "exam_objective_answer_check_batch")
  public ApiResponse<Void> resume(@PathVariable Long batchId) {
    PlatformUserPrincipal principal = requirePlatformAdmin();
    service.resume(batchId, principal.getTenantId(), principal.getUserId());
    return ApiResponse.ok();
  }

  @PostMapping("/batches/{batchId}/retry-failed")
  @PreAuthorize("hasAuthority('exam:objective-answer-check:submit')")
  @AuditLog(module = "exam", action = "objectiveAnswerCheck:retryFailed",
      targetType = "exam_objective_answer_check_batch")
  public ApiResponse<Map<String, Integer>> retryFailed(@PathVariable Long batchId) {
    PlatformUserPrincipal principal = requirePlatformAdmin();
    int reset = service.retryFailed(batchId, principal.getTenantId(), principal.getUserId());
    return ApiResponse.ok(Map.of("resetCount", reset));
  }

  @GetMapping("/batches/{batchId}/items")
  @PreAuthorize("hasAuthority('exam:objective-answer-check:view')")
  public ApiResponse<PageResponse<ObjectiveAnswerItemVO>> items(
      @PathVariable Long batchId,
      @RequestParam(required = false) String resultCode,
      @RequestParam(required = false) String contentQuality,
      @RequestParam(required = false) Boolean needsReview,
      @RequestParam(required = false) String reviewStatus,
      @RequestParam(required = false) String questionType,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long pageSize) {
    requirePlatformAdmin();
    ObjectiveAnswerItemFilter filter = new ObjectiveAnswerItemFilter();
    filter.setResultCode(resultCode);
    filter.setContentQuality(contentQuality);
    filter.setNeedsReview(needsReview);
    filter.setReviewStatus(reviewStatus);
    filter.setQuestionType(questionType);
    filter.setKeyword(keyword);
    return ApiResponse.ok(service.pageItems(batchId, filter, page, pageSize));
  }

  @GetMapping("/batches/{batchId}/items/{itemId}")
  @PreAuthorize("hasAuthority('exam:objective-answer-check:view')")
  public ApiResponse<ObjectiveAnswerItemDetailVO> item(@PathVariable Long batchId,
      @PathVariable Long itemId) {
    requirePlatformAdmin();
    return ApiResponse.ok(service.getItem(batchId, itemId));
  }

  @PostMapping("/batches/{batchId}/items/{itemId}/review")
  @PreAuthorize("hasAuthority('exam:objective-answer-check:review')")
  @AuditLog(module = "exam", action = "objectiveAnswerCheck:review",
      targetType = "exam_objective_answer_check_item")
  public ApiResponse<Void> review(@PathVariable Long batchId, @PathVariable Long itemId,
      @Valid @RequestBody ObjectiveAnswerReviewReq req) {
    PlatformUserPrincipal principal = requirePlatformAdmin();
    service.review(batchId, itemId, req, principal.getUserId());
    return ApiResponse.ok();
  }

  @GetMapping("/batches/{batchId}/export")
  @PreAuthorize("hasAuthority('exam:objective-answer-check:export')")
  @AuditLog(module = "exam", action = "objectiveAnswerCheck:export",
      targetType = "exam_objective_answer_check_batch")
  public ResponseEntity<byte[]> export(@PathVariable Long batchId,
      @RequestParam(required = false) String resultCode,
      @RequestParam(required = false) String contentQuality,
      @RequestParam(required = false) Boolean needsReview,
      @RequestParam(required = false) String reviewStatus,
      @RequestParam(required = false) String questionType,
      @RequestParam(required = false) String keyword) {
    requirePlatformAdmin();
    ObjectiveAnswerItemFilter filter = new ObjectiveAnswerItemFilter();
    filter.setResultCode(resultCode);
    filter.setContentQuality(contentQuality);
    filter.setNeedsReview(needsReview);
    filter.setReviewStatus(reviewStatus);
    filter.setQuestionType(questionType);
    filter.setKeyword(keyword);
    ExportResultDTO result = service.export(batchId, filter);
    String encoded = URLEncoder.encode(result.filename(), StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + result.filename() + "\"; filename*=UTF-8''" + encoded)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(result.data());
  }

  /** 一期硬约束：非平台管理员即使有同名权限码也 403。 */
  private PlatformUserPrincipal requirePlatformAdmin() {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    if (!PlatformAdminSupport.isPlatformAdmin(principal.getRoleCodes())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "仅平台管理员可以使用答案 AI 检查");
    }
    return principal;
  }
}
