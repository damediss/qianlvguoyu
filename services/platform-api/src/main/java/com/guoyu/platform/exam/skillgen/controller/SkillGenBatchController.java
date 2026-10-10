package com.guoyu.platform.exam.skillgen.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.skillgen.dto.SkillGenBatchVO;
import com.guoyu.platform.exam.skillgen.dto.SkillModelConfigVO;
import com.guoyu.platform.exam.skillgen.service.SkillGenBatchService;
import com.guoyu.platform.security.SecurityContext;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 操作技能试题生成批次（提交/进度/暂停/继续/重试） */
@RestController
@RequestMapping("/exam/skill/gen")
@Slf4j
public class SkillGenBatchController {

  private final SkillGenBatchService batchService;

  public SkillGenBatchController(SkillGenBatchService batchService) {
    this.batchService = batchService;
  }

  /** 提交生成批次（秒回 batchId；每考评点固定 5 题） */
  @PostMapping("/submit")
  @PreAuthorize("hasAuthority('exam:skill:gen:submit')")
  @AuditLog(module = "exam", action = "skill:gen:submit", targetType = "exam_skill_gen_batch")
  public ApiResponse<SkillGenBatchVO> submit(@RequestBody SubmitReq req) {
    Long tenantId = TenantContextHolder.getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    return ApiResponse.ok(batchService.submit(tenantId, userId, req.blueprintId(), req.modelConfigId()));
  }

  /** 批次分页 */
  @GetMapping("/page")
  @PreAuthorize("hasAuthority('exam:skill:gen:view')")
  public ApiResponse<PageResponse<SkillGenBatchVO>> page(
      @RequestParam(required = false) Long blueprintId,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "10") long pageSize) {
    Long tenantId = TenantContextHolder.getTenantId();
    List<String> statusList = status == null || status.isBlank()
        ? List.of()
        : java.util.Arrays.stream(status.split(",")).map(String::trim)
            .filter(s -> !s.isEmpty()).toList();
    return ApiResponse.ok(batchService.page(tenantId, blueprintId, statusList, page, pageSize));
  }

  /** 启用中的模型配置（前端下拉推荐值，默认选中 skill-gen-glm） */
  @GetMapping("/model-configs")
  @PreAuthorize("hasAuthority('exam:skill:gen:view')")
  public ApiResponse<List<SkillModelConfigVO>> modelConfigs() {
    Long tenantId = TenantContextHolder.getTenantId();
    return ApiResponse.ok(batchService.listEnabledModelConfigs(tenantId));
  }

  /** 批次进度（前端轮询） */
  @GetMapping("/{batchId}/progress")
  @PreAuthorize("hasAuthority('exam:skill:gen:view')")
  public ApiResponse<SkillGenBatchVO> progress(@PathVariable Long batchId) {
    Long tenantId = TenantContextHolder.getTenantId();
    return ApiResponse.ok(batchService.getProgress(tenantId, batchId));
  }

  /** 暂停 */
  @PostMapping("/{batchId}/pause")
  @PreAuthorize("hasAuthority('exam:skill:gen:pause')")
  @AuditLog(module = "exam", action = "skill:gen:pause", targetType = "exam_skill_gen_batch")
  public ApiResponse<SkillGenBatchVO> pause(@PathVariable Long batchId) {
    return ApiResponse.ok(batchService.pause(TenantContextHolder.getTenantId(), batchId));
  }

  /** 继续 */
  @PostMapping("/{batchId}/resume")
  @PreAuthorize("hasAuthority('exam:skill:gen:pause')")
  @AuditLog(module = "exam", action = "skill:gen:resume", targetType = "exam_skill_gen_batch")
  public ApiResponse<SkillGenBatchVO> resume(@PathVariable Long batchId) {
    Long tenantId = TenantContextHolder.getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    return ApiResponse.ok(batchService.resume(tenantId, userId, batchId));
  }

  /** 重试失败块/补跑缺口 */
  @PostMapping("/{batchId}/retry-failed")
  @PreAuthorize("hasAuthority('exam:skill:gen:pause')")
  @AuditLog(module = "exam", action = "skill:gen:retry", targetType = "exam_skill_gen_batch")
  public ApiResponse<SkillGenBatchVO> retryFailed(@PathVariable Long batchId) {
    Long tenantId = TenantContextHolder.getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    return ApiResponse.ok(batchService.retryFailed(tenantId, userId, batchId));
  }

  /** 提交请求体 */
  public record SubmitReq(Long blueprintId, Long modelConfigId) {
  }
}
