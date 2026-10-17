package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.web.DownloadHeaders;
import com.guoyu.platform.exam.dto.FaqConversionQueryReq;
import com.guoyu.platform.exam.dto.FaqConversionSubmitReq;
import com.guoyu.platform.exam.service.FaqConversionApplicationService;
import com.guoyu.platform.exam.service.FaqConversionBatchService;
import com.guoyu.platform.exam.service.FaqModelConfigApplicationService;
import com.guoyu.platform.exam.vo.FaqBatchVO;
import com.guoyu.platform.exam.vo.FaqConversionVO;
import com.guoyu.platform.common.api.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** FAQ 转换 REST API */
@Validated
@RestController
@RequestMapping("/exam/faq/conversion")
@Slf4j
public class ExamFaqConversionController {

  private final FaqConversionApplicationService conversionService;
  private final FaqConversionBatchService batchService;
  private final FaqModelConfigApplicationService modelConfigService;

  public ExamFaqConversionController(
      FaqConversionApplicationService conversionService,
      FaqConversionBatchService batchService,
      FaqModelConfigApplicationService modelConfigService) {
    this.conversionService = conversionService;
    this.batchService = batchService;
    this.modelConfigService = modelConfigService;
  }

  /** 发起 FAQ 转换 */
  @PostMapping("/submit")
  @PreAuthorize("hasAuthority('exam:faq:generate')")
  @AuditLog(module = "exam", action = "faq:conversion:submit", targetType = "exam_faq_conversion_batch")
  public ApiResponse<FaqBatchVO> submit(@Valid @RequestBody FaqConversionSubmitReq req) {
    return ApiResponse.ok(conversionService.submitConversion(req));
  }

  /** 查询批次列表 */
  @GetMapping("/batches")
  @PreAuthorize("hasAuthority('exam:faq:view')")
  public ApiResponse<PageResponse<FaqBatchVO>> listBatches(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") Long page,
      @RequestParam(defaultValue = "10") Long pageSize) {
    Long tenantId = com.guoyu.platform.security.SecurityContext.currentUser().getTenantId();
    return ApiResponse.ok(batchService.page(tenantId, status, page, pageSize));
  }

  /** 查询批次进度（前端轮询） */
  @GetMapping("/batches/{batchId}/progress")
  @PreAuthorize("hasAuthority('exam:faq:view')")
  public ApiResponse<FaqBatchVO> getProgress(@PathVariable Long batchId) {
    Long tenantId = com.guoyu.platform.security.SecurityContext.currentUser().getTenantId();
    return ApiResponse.ok(conversionService.getBatchProgress(tenantId, batchId));
  }

  /** 查询转换记录分页 */
  @GetMapping("/records")
  @PreAuthorize("hasAuthority('exam:faq:view')")
  public ApiResponse<PageResponse<FaqConversionVO>> pageRecords(@Valid @ModelAttribute FaqConversionQueryReq req) {
    return ApiResponse.ok(conversionService.pageRecords(req));
  }

  /** 查单条转换记录详情 */
  @GetMapping("/records/{id}")
  @PreAuthorize("hasAuthority('exam:faq:view')")
  public ApiResponse<Map<String, Object>> getDetail(@PathVariable Long id) {
    Long tenantId = com.guoyu.platform.security.SecurityContext.currentUser().getTenantId();
    return ApiResponse.ok(conversionService.getDetail(tenantId, id));
  }

  /** 暂停批次 */
  @PostMapping("/batches/{batchId}/pause")
  @PreAuthorize("hasAuthority('exam:faq:pause')")
  @AuditLog(module = "exam", action = "faq:batch:pause", targetType = "exam_faq_conversion_batch")
  public ApiResponse<Void> pause(@PathVariable Long batchId) {
    Long tenantId = com.guoyu.platform.security.SecurityContext.currentUser().getTenantId();
    batchService.pauseBatch(tenantId, batchId);
    return ApiResponse.ok();
  }

  /** 恢复批次：改状态 + 重新启动异步 LLM 处理 */
  @PostMapping("/batches/{batchId}/resume")
  @PreAuthorize("hasAuthority('exam:faq:pause')")
  @AuditLog(module = "exam", action = "faq:batch:resume", targetType = "exam_faq_conversion_batch")
  public ApiResponse<Void> resume(@PathVariable Long batchId) {
    Long tenantId = com.guoyu.platform.security.SecurityContext.currentUser().getTenantId();
    Long userId = com.guoyu.platform.security.SecurityContext.currentUser().getUserId();
    conversionService.resumeBatch(tenantId, userId, batchId);
    return ApiResponse.ok();
  }

  /** 重试失败记录 */
  @PostMapping("/batches/{batchId}/retry")
  @PreAuthorize("hasAuthority('exam:faq:retry')")
  @AuditLog(module = "exam", action = "faq:batch:retry", targetType = "exam_faq_conversion")
  public ApiResponse<Void> retryFailed(@PathVariable Long batchId) {
    Long tenantId = com.guoyu.platform.security.SecurityContext.currentUser().getTenantId();
    Long userId = com.guoyu.platform.security.SecurityContext.currentUser().getUserId();
    conversionService.retryFailed(tenantId, userId, batchId);
    return ApiResponse.ok();
  }

  /** 导出批次成功的 FAQ 为 zip 文件 */
  @GetMapping("/batches/{batchId}/export")
  @PreAuthorize("hasAuthority('exam:faq:export')")
  @AuditLog(module = "exam", action = "faq:batch:export", targetType = "exam_faq_conversion")
  public ResponseEntity<InputStreamResource> export(@PathVariable Long batchId) {
    Long tenantId = com.guoyu.platform.security.SecurityContext.currentUser().getTenantId();
    byte[] zipBytes = conversionService.exportBatch(tenantId, batchId);
    String filename = "faq_batch_" + batchId + ".zip";
    return ResponseEntity.ok()
        .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "application/zip")
        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
            DownloadHeaders.contentDispositionAttachment(filename))
        .body(new org.springframework.core.io.InputStreamResource(new java.io.ByteArrayInputStream(zipBytes)));
  }

  /** 查询可用模型列表（供下拉选择） */
  @GetMapping("/models/enabled")
  @PreAuthorize("hasAuthority('exam:faq:view')")
  public ApiResponse<List<com.guoyu.platform.exam.vo.FaqModelConfigVO>> listEnabledModels() {
    return ApiResponse.ok(modelConfigService.listEnabled());
  }
}
