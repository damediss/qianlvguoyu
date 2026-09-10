package com.guoyu.platform.exam.skillgen.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.skillgen.dto.SkillBlueprintTreeVO;
import com.guoyu.platform.exam.skillgen.dto.SkillBlueprintVO;
import com.guoyu.platform.exam.skillgen.service.SkillBlueprintApplicationService;
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

/** 操作技能细目表批次（导入/查询/树/删除/源表下载） */
@RestController
@RequestMapping("/exam/skill/blueprint")
@Slf4j
public class SkillBlueprintController {

  private final SkillBlueprintApplicationService blueprintService;
  private final com.guoyu.platform.file.service.FileApplicationService fileApplicationService;

  public SkillBlueprintController(SkillBlueprintApplicationService blueprintService,
      com.guoyu.platform.file.service.FileApplicationService fileApplicationService) {
    this.blueprintService = blueprintService;
    this.fileApplicationService = fileApplicationService;
  }

  /** 导入操作技能细目表 xlsx */
  @PostMapping("/import")
  @PreAuthorize("hasAuthority('exam:skill:blueprint:import')")
  @AuditLog(module = "exam", action = "skill:blueprint:import", targetType = "exam_skill_blueprint")
  public ApiResponse<SkillBlueprintVO> importBlueprint(
      @RequestParam("file") MultipartFile file,
      @RequestParam("workTypeId") Integer workTypeId,
      @RequestParam("level") Integer level) {
    return ApiResponse.ok(blueprintService.importBlueprint(file, workTypeId, level));
  }

  /** 批次分页 */
  @GetMapping("/page")
  @PreAuthorize("hasAuthority('exam:skill:blueprint:view')")
  public ApiResponse<PageResponse<SkillBlueprintVO>> page(
      @RequestParam(required = false) Integer workTypeId,
      @RequestParam(required = false) Integer level,
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "10") long pageSize) {
    Long tenantId = TenantContextHolder.getTenantId();
    return ApiResponse.ok(blueprintService.page(tenantId, workTypeId, level, page, pageSize));
  }

  /** 批次树 */
  @GetMapping("/{id}/tree")
  @PreAuthorize("hasAuthority('exam:skill:blueprint:view')")
  public ApiResponse<SkillBlueprintTreeVO> tree(@PathVariable Long id) {
    Long tenantId = TenantContextHolder.getTenantId();
    return ApiResponse.ok(blueprintService.tree(tenantId, id));
  }

  /** 源细目表下载（导入时留存，供事后核对） */
  @GetMapping("/{id}/source-file")
  @PreAuthorize("hasAuthority('exam:skill:blueprint:view')")
  public org.springframework.http.ResponseEntity<byte[]> sourceFile(@PathVariable Long id) {
    Long tenantId = TenantContextHolder.getTenantId();
    var bp = blueprintService.findBlueprint(tenantId, id);
    if (bp == null) {
      throw new com.guoyu.platform.common.exception.BusinessException(
          com.guoyu.platform.common.exception.ErrorCode.NOT_FOUND, "细目表批次不存在");
    }
    if (bp.getSourceFileId() == null) {
      throw new com.guoyu.platform.common.exception.BusinessException(
          com.guoyu.platform.common.exception.ErrorCode.NOT_FOUND, "该批次未留存源文件，请重新导入");
    }
    var file = fileApplicationService.readFileBytesById(bp.getSourceFileId());
    String name = bp.getSourceFileName() == null ? "细目表.xlsx" : bp.getSourceFileName();
    String encoded = java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8)
        .replace("+", "%20");
    return org.springframework.http.ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename*=UTF-8''" + encoded)
        .header("Content-Type",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        .body(file);
  }

  /** 删除批次（软删；已有生成批次的不可删） */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('exam:skill:blueprint:import')")
  @AuditLog(module = "exam", action = "skill:blueprint:delete", targetType = "exam_skill_blueprint")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    Long tenantId = TenantContextHolder.getTenantId();
    blueprintService.delete(tenantId, id);
    return ApiResponse.ok();
  }
}
