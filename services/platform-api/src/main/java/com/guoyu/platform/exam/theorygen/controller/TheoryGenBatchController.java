package com.guoyu.platform.exam.theorygen.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.theorygen.dto.TheoryGenBatchVO;
import com.guoyu.platform.exam.theorygen.service.TheoryGenBatchService;
import com.guoyu.platform.security.SecurityContext;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 理论试题生成批次（提交/进度/暂停/继续/重试） */
@RestController
@RequestMapping("/exam/theory/gen")
@Slf4j
public class TheoryGenBatchController {

  private final TheoryGenBatchService batchService;

  public TheoryGenBatchController(TheoryGenBatchService batchService) {
    this.batchService = batchService;
  }

  /** 提交生成批次（秒回 batchId） */
  @PostMapping("/submit")
  @PreAuthorize("hasAuthority('exam:theory:gen:submit')")
  @AuditLog(module = "exam", action = "theory:gen:submit", targetType = "exam_theory_gen_batch")
  public ApiResponse<TheoryGenBatchVO> submit(@RequestBody SubmitReq req) {
    Long tenantId = TenantContextHolder.getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    return ApiResponse.ok(batchService.submit(tenantId, userId, req.blueprintId(),
        req.modelConfigId(), req.quota()));
  }

  /** 批次分页 */
  @GetMapping("/page")
  @PreAuthorize("hasAuthority('exam:theory:gen:view')")
  public ApiResponse<PageResponse<TheoryGenBatchVO>> page(
      @RequestParam(required = false) Long blueprintId,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "10") long pageSize) {
    Long tenantId = TenantContextHolder.getTenantId();
    // status 逗号分隔（如 pending,running）：工作台进行中区置顶查询用
    java.util.List<String> statusList = status == null || status.isBlank()
        ? java.util.List.of()
        : java.util.Arrays.stream(status.split(",")).map(String::trim)
            .filter(s -> !s.isEmpty()).toList();
    return ApiResponse.ok(batchService.page(tenantId, blueprintId, statusList, page, pageSize));
  }

  /** 启用中的模型配置（前端下拉推荐值，默认选中 theory-gen-glm） */
  @GetMapping("/model-configs")
  @PreAuthorize("hasAuthority('exam:theory:gen:view')")
  public ApiResponse<java.util.List<com.guoyu.platform.exam.theorygen.dto.TheoryModelConfigVO>> modelConfigs() {
    Long tenantId = TenantContextHolder.getTenantId();
    return ApiResponse.ok(batchService.listEnabledModelConfigs(tenantId));
  }

  /** 批次进度（前端轮询） */
  @GetMapping("/{batchId}/progress")
  @PreAuthorize("hasAuthority('exam:theory:gen:view')")
  public ApiResponse<TheoryGenBatchVO> progress(@PathVariable Long batchId) {
    Long tenantId = TenantContextHolder.getTenantId();
    return ApiResponse.ok(batchService.getProgress(tenantId, batchId));
  }

  /** 暂停 */
  @PostMapping("/{batchId}/pause")
  @PreAuthorize("hasAuthority('exam:theory:gen:pause')")
  @AuditLog(module = "exam", action = "theory:gen:pause", targetType = "exam_theory_gen_batch")
  public ApiResponse<TheoryGenBatchVO> pause(@PathVariable Long batchId) {
    return ApiResponse.ok(batchService.pause(TenantContextHolder.getTenantId(), batchId));
  }

  /** 继续 */
  @PostMapping("/{batchId}/resume")
  @PreAuthorize("hasAuthority('exam:theory:gen:pause')")
  @AuditLog(module = "exam", action = "theory:gen:resume", targetType = "exam_theory_gen_batch")
  public ApiResponse<TheoryGenBatchVO> resume(@PathVariable Long batchId) {
    Long tenantId = TenantContextHolder.getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    return ApiResponse.ok(batchService.resume(tenantId, userId, batchId));
  }

  /** 重试失败块 */
  @PostMapping("/{batchId}/retry-failed")
  @PreAuthorize("hasAuthority('exam:theory:gen:pause')")
  @AuditLog(module = "exam", action = "theory:gen:retry", targetType = "exam_theory_gen_batch")
  public ApiResponse<TheoryGenBatchVO> retryFailed(@PathVariable Long batchId) {
    Long tenantId = TenantContextHolder.getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    return ApiResponse.ok(batchService.retryFailed(tenantId, userId, batchId));
  }

  /** 提交请求体 */
  public record SubmitReq(Long blueprintId, Long modelConfigId, Map<String, Integer> quota) {
  }
}
