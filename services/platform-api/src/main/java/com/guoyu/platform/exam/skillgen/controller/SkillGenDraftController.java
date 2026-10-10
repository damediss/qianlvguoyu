package com.guoyu.platform.exam.skillgen.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.skillgen.dto.SkillDraftVO;
import com.guoyu.platform.exam.skillgen.dto.SkillPublishReq;
import com.guoyu.platform.exam.skillgen.dto.SkillPublishResultVO;
import com.guoyu.platform.exam.skillgen.entity.SkillGenDraftEntity;
import com.guoyu.platform.exam.skillgen.service.SkillGenDraftService;
import com.guoyu.platform.exam.skillgen.service.SkillGenExportService;
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

/** 操作技能试题草稿：查询/编辑/审核/入库/导出 */
@RestController
@RequestMapping("/exam/skill/draft")
@Slf4j
public class SkillGenDraftController {

  private final SkillGenDraftService draftService;
  private final SkillGenExportService exportService;

  public SkillGenDraftController(SkillGenDraftService draftService,
      SkillGenExportService exportService) {
    this.draftService = draftService;
    this.exportService = exportService;
  }

  /** 草稿分页（batchId 可选：不传为"全部批次"模式，按创建时间倒序） */
  @GetMapping("/page")
  @PreAuthorize("hasAuthority('exam:skill:draft:view')")
  public ApiResponse<PageResponse<SkillDraftVO>> page(
      @RequestParam(required = false) Long batchId,
      @RequestParam(required = false) String reviewStatus,
      @RequestParam(required = false) Integer publishStatus,
      @RequestParam(required = false) Long nodeId,
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long pageSize) {
    Long tenantId = TenantContextHolder.getTenantId();
    return ApiResponse.ok(draftService.page(tenantId, batchId, reviewStatus, publishStatus,
        nodeId, page, pageSize));
  }

  /** 编辑草稿（保存后自动退回待审核状态） */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('exam:skill:draft:edit')")
  public ApiResponse<SkillDraftVO> edit(@PathVariable Long id,
      @RequestBody SkillGenDraftEntity patch) {
    Long tenantId = TenantContextHolder.getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    return ApiResponse.ok(draftService.edit(tenantId, userId, id, patch));
  }

  /** 批量审核 */
  @PostMapping("/review")
  @PreAuthorize("hasAuthority('exam:skill:draft:review')")
  @AuditLog(module = "exam", action = "skill:draft:review", targetType = "exam_skill_gen_draft")
  public ApiResponse<Integer> review(@Valid @RequestBody ReviewReq req) {
    Long tenantId = TenantContextHolder.getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    return ApiResponse.ok(draftService.review(tenantId, userId, req.draftIds(), req.approve()));
  }

  /** 入库预览（前端闸门 UI：本次可入库数、累计进度 vs 等级下限、缺口） */
  @GetMapping("/publish-preview")
  @PreAuthorize("hasAuthority('exam:skill:draft:view')")
  public ApiResponse<java.util.Map<String, Object>> publishPreview(@RequestParam Long batchId) {
    Long tenantId = TenantContextHolder.getTenantId();
    return ApiResponse.ok(draftService.publishPreview(tenantId, batchId));
  }

  /** 人工入库（硬闸门：仅审核通过 + 累计总量 ≥ 等级下限） */
  @PostMapping("/publish")
  @PreAuthorize("hasAuthority('exam:skill:draft:publish')")
  @AuditLog(module = "exam", action = "skill:draft:publish", targetType = "exam_skill_gen_batch")
  public ApiResponse<SkillPublishResultVO> publish(@Valid @RequestBody SkillPublishReq req) {
    Long tenantId = TenantContextHolder.getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    return ApiResponse.ok(draftService.publish(tenantId, userId, req));
  }

  /** 导出批次试题表 xlsx（试题表 + 评分记录表 + 批次报告 三 Sheet） */
  @GetMapping("/export")
  @PreAuthorize("hasAnyAuthority('exam:skill:gen:export', 'exam:skill:draft:export')")
  public org.springframework.http.ResponseEntity<byte[]> export(@RequestParam Long batchId)
      throws java.io.IOException {
    Long tenantId = TenantContextHolder.getTenantId();
    var file = exportService.exportBatch(tenantId, batchId);
    // RFC 5987：中文文件名用 filename* 编码
    String encoded = java.net.URLEncoder.encode(file.filename(),
        java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    return org.springframework.http.ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename*=UTF-8''" + encoded)
        .header("Content-Type",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        .body(file.content());
  }

  public record ReviewReq(List<Long> draftIds, boolean approve) {
  }
}
