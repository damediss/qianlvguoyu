package com.guoyu.platform.exam.theorygen.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.theorygen.dto.TheoryDraftVO;
import com.guoyu.platform.exam.theorygen.dto.TheoryPublishReq;
import com.guoyu.platform.exam.theorygen.dto.TheoryPublishResultVO;
import com.guoyu.platform.exam.theorygen.entity.TheoryGenDraftEntity;
import com.guoyu.platform.exam.theorygen.service.TheoryGenDraftService;
import com.guoyu.platform.security.SecurityContext;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 试题草稿：查询/编辑/审核/入库 */
@RestController
@RequestMapping("/exam/theory/draft")
@Slf4j
public class TheoryGenDraftController {

  private final TheoryGenDraftService draftService;
  private final com.guoyu.platform.exam.theorygen.service.TheoryGenExportService exportService;

  public TheoryGenDraftController(TheoryGenDraftService draftService,
      com.guoyu.platform.exam.theorygen.service.TheoryGenExportService exportService) {
    this.draftService = draftService;
    this.exportService = exportService;
  }

  /** 草稿分页 */
  @GetMapping("/page")
  @PreAuthorize("hasAuthority('exam:theory:draft:view')")
  public ApiResponse<PageResponse<TheoryDraftVO>> page(
      @RequestParam Long batchId,
      @RequestParam(required = false) String questionType,
      @RequestParam(required = false) String reviewStatus,
      @RequestParam(required = false) Integer publishStatus,
      @RequestParam(required = false) Long nodeId,
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long pageSize) {
    Long tenantId = TenantContextHolder.getTenantId();
    return ApiResponse.ok(draftService.page(tenantId, batchId, questionType, reviewStatus,
        publishStatus, nodeId, page, pageSize));
  }

  /** 编辑草稿 */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('exam:theory:draft:edit')")
  public ApiResponse<TheoryDraftVO> edit(@PathVariable Long id,
      @RequestBody TheoryGenDraftEntity patch) {
    Long tenantId = TenantContextHolder.getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    return ApiResponse.ok(draftService.edit(tenantId, userId, id, patch));
  }

  /** 批量审核 */
  @PostMapping("/review")
  @PreAuthorize("hasAuthority('exam:theory:draft:review')")
  @AuditLog(module = "exam", action = "theory:draft:review", targetType = "exam_theory_gen_draft")
  public ApiResponse<Integer> review(@Valid @RequestBody ReviewReq req) {
    Long tenantId = TenantContextHolder.getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    return ApiResponse.ok(draftService.review(tenantId, userId, req.draftIds(), req.approve()));
  }

  /** 人工入库 */
  @PostMapping("/publish")
  @PreAuthorize("hasAuthority('exam:theory:draft:publish')")
  @AuditLog(module = "exam", action = "theory:draft:publish", targetType = "exam_theory_gen_batch")
  public ApiResponse<TheoryPublishResultVO> publish(@Valid @RequestBody TheoryPublishReq req) {
    Long tenantId = TenantContextHolder.getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    return ApiResponse.ok(draftService.publish(tenantId, userId, req));
  }

  /** 导出批次试题表 xlsx */
  @GetMapping("/export")
  @PreAuthorize("hasAuthority('exam:theory:gen:export')")
  public org.springframework.http.ResponseEntity<byte[]> export(@RequestParam Long batchId)
      throws java.io.IOException {
    Long tenantId = TenantContextHolder.getTenantId();
    var file = exportService.exportBatch(tenantId, batchId);
    // RFC 5987：中文文件名用 filename* 编码
    String encoded = java.net.URLEncoder.encode(file.filename(), java.nio.charset.StandardCharsets.UTF_8)
        .replace("+", "%20");
    return org.springframework.http.ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename*=UTF-8''" + encoded)
        .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        .body(file.content());
  }

  public record ReviewReq(List<Long> draftIds, boolean approve) {
  }
}
