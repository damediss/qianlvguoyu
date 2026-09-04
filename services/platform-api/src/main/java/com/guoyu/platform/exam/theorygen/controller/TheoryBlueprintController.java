package com.guoyu.platform.exam.theorygen.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.theorygen.dto.BlueprintTreeVO;
import com.guoyu.platform.exam.theorygen.dto.BlueprintVO;
import com.guoyu.platform.exam.theorygen.service.TheoryBlueprintApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 理论细目表批次（导入/查询/删除） */
@RestController
@RequestMapping("/exam/theory/blueprint")
@Slf4j
public class TheoryBlueprintController {

  private final TheoryBlueprintApplicationService blueprintService;

  public TheoryBlueprintController(TheoryBlueprintApplicationService blueprintService) {
    this.blueprintService = blueprintService;
  }

  /** 导入细目表 xlsx */
  @PostMapping("/import")
  @PreAuthorize("hasAuthority('exam:theory:blueprint:import')")
  @AuditLog(module = "exam", action = "theory:blueprint:import", targetType = "exam_theory_blueprint")
  public ApiResponse<BlueprintVO> importBlueprint(
      @RequestParam("file") MultipartFile file,
      @RequestParam("workTypeId") Integer workTypeId,
      @RequestParam("level") Integer level) {
    return ApiResponse.ok(blueprintService.importBlueprint(file, workTypeId, level));
  }

  /** 批次分页 */
  @GetMapping("/page")
  @PreAuthorize("hasAuthority('exam:theory:blueprint:view')")
  public ApiResponse<PageResponse<BlueprintVO>> page(
      @RequestParam(required = false) Integer workTypeId,
      @RequestParam(required = false) Integer level,
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "10") long pageSize) {
    Long tenantId = TenantContextHolder.getTenantId();
    return ApiResponse.ok(blueprintService.page(tenantId, workTypeId, level, page, pageSize));
  }

  /** 批次树 */
  @GetMapping("/{id}/tree")
  @PreAuthorize("hasAuthority('exam:theory:blueprint:view')")
  public ApiResponse<BlueprintTreeVO> tree(@PathVariable Long id) {
    Long tenantId = TenantContextHolder.getTenantId();
    return ApiResponse.ok(blueprintService.tree(tenantId, id));
  }

  /** 删除批次（软删） */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('exam:theory:blueprint:import')")
  @AuditLog(module = "exam", action = "theory:blueprint:delete", targetType = "exam_theory_blueprint")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    Long tenantId = TenantContextHolder.getTenantId();
    blueprintService.delete(tenantId, id);
    return ApiResponse.ok();
  }
}
