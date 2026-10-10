package com.guoyu.platform.exam.standard.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.standard.dto.StandardNodeCreateReq;
import com.guoyu.platform.exam.standard.dto.StandardNodeUpdateReq;
import com.guoyu.platform.exam.standard.dto.StandardSummarizeReq;
import com.guoyu.platform.exam.standard.service.StandardExportApplicationService;
import com.guoyu.platform.exam.standard.service.StandardExportApplicationService.ExportFile;
import com.guoyu.platform.exam.standard.service.StandardLlmSummarizeService;
import com.guoyu.platform.exam.standard.service.StandardParseApplicationService;
import com.guoyu.platform.exam.standard.vo.StandardNodeVO;
import com.guoyu.platform.exam.standard.vo.StandardTaskVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 细目表管理（方案 §11）：/api/exam/standard/**，权限码 ximubiaoguanli:*。
 */
@Validated
@RestController
@RequestMapping("/exam/standard")
@Slf4j
public class StandardParseController {

  private final StandardParseApplicationService parseService;
  private final StandardLlmSummarizeService llmService;
  private final StandardExportApplicationService exportService;

  public StandardParseController(StandardParseApplicationService parseService,
      StandardLlmSummarizeService llmService,
      StandardExportApplicationService exportService) {
    this.parseService = parseService;
    this.llmService = llmService;
    this.exportService = exportService;
  }

  /** #1 上传 docx 并同步解析骨架 */
  @PostMapping(value = "/tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('ximubiaoguanli:generate')")
  @AuditLog(module = "exam", action = "standard:upload", targetType = "exam_standard_task")
  public ApiResponse<StandardTaskVO> upload(@RequestParam("file") MultipartFile file)
      throws IOException {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件为空");
    }
    String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
    if (!name.endsWith(".docx")) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 .docx 文件");
    }
    PlatformUserPrincipal user = SecurityContext.currentUser();
    return ApiResponse.ok(parseService.uploadAndParse(file.getBytes(),
        file.getOriginalFilename(), user.getTenantId(), user.getUserId()));
  }

  /** #2 任务分页列表 */
  @GetMapping("/tasks")
  @PreAuthorize("hasAuthority('ximubiaoguanli:view')")
  public ApiResponse<PageResponse<StandardTaskVO>> page(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long pageSize,
      @RequestParam(required = false) String keyword) {
    return ApiResponse.ok(parseService.page(page, pageSize, keyword,
        SecurityContext.currentUser().getTenantId()));
  }

  /** 任务详情（含警告） */
  @GetMapping("/tasks/{id}")
  @PreAuthorize("hasAuthority('ximubiaoguanli:view')")
  public ApiResponse<StandardTaskVO> getTask(@PathVariable Long id) {
    return ApiResponse.ok(parseService.getTask(id, SecurityContext.currentUser().getTenantId()));
  }

  /** #3 触发 LLM 概括（异步；重复触发 409；body 可选：scopes=工种×等级，importants=XYZ 档位） */
  @PostMapping("/tasks/{id}/summarize")
  @PreAuthorize("hasAuthority('ximubiaoguanli:generate')")
  @AuditLog(module = "exam", action = "standard:summarize", targetType = "exam_standard_task")
  public ResponseEntity<ApiResponse<Long>> summarize(@PathVariable Long id,
      @RequestBody(required = false) StandardSummarizeReq req) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    Long taskId = llmService.triggerSummarize(id, user.getTenantId(), user.getUserId(), req);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(taskId));
  }

  /** #4 树查询（等级×方向，dimension 过滤叶维度） */
  @GetMapping("/tasks/{id}/tree")
  @PreAuthorize("hasAuthority('ximubiaoguanli:view')")
  public ApiResponse<List<StandardNodeVO>> tree(@PathVariable Long id,
      @RequestParam(required = false) Integer levelCode,
      @RequestParam(required = false) String direction,
      @RequestParam(required = false) String dimension) {
    return ApiResponse.ok(parseService.tree(id, levelCode, direction, dimension,
        SecurityContext.currentUser().getTenantId()));
  }

  /** #5 节点编辑 */
  @PutMapping("/nodes/{id}")
  @PreAuthorize("hasAuthority('ximubiaoguanli:generate')")
  @AuditLog(module = "exam", action = "standard:node-update", targetType = "exam_standard_node")
  public ApiResponse<StandardNodeVO> updateNode(@PathVariable Long id,
      @Valid @RequestBody StandardNodeUpdateReq req) {
    return ApiResponse.ok(parseService.updateNode(id, req.title(), req.important(), req.weight(),
        req.sortNumber(), SecurityContext.currentUser().getTenantId()));
  }

  /** #6 人工新增叶节点 */
  @PostMapping("/nodes")
  @PreAuthorize("hasAuthority('ximubiaoguanli:generate')")
  @AuditLog(module = "exam", action = "standard:node-create", targetType = "exam_standard_node")
  public ApiResponse<StandardNodeVO> createNode(@Valid @RequestBody StandardNodeCreateReq req) {
    return ApiResponse.ok(parseService.createNode(req.taskId(), req.parentId(), req.title(),
        req.important(), req.dimension(), SecurityContext.currentUser().getTenantId()));
  }

  /** #6 删除节点（含子树） */
  @DeleteMapping("/nodes/{id}")
  @PreAuthorize("hasAuthority('ximubiaoguanli:generate')")
  @AuditLog(module = "exam", action = "standard:node-delete", targetType = "exam_standard_node")
  public ApiResponse<Void> deleteNode(@PathVariable Long id) {
    parseService.deleteNode(id, SecurityContext.currentUser().getTenantId());
    return ApiResponse.ok();
  }

  /** 节点上移/下移 */
  @PostMapping("/nodes/{id}/move")
  @PreAuthorize("hasAuthority('ximubiaoguanli:generate')")
  public ApiResponse<Void> moveNode(@PathVariable Long id, @RequestParam String direction) {
    parseService.moveNode(id, "up".equalsIgnoreCase(direction),
        SecurityContext.currentUser().getTenantId());
    return ApiResponse.ok();
  }

  /** #7 单小节重新生成 */
  @PostMapping("/nodes/{id}/resummarize")
  @PreAuthorize("hasAuthority('ximubiaoguanli:generate')")
  @AuditLog(module = "exam", action = "standard:resummarize", targetType = "exam_standard_node")
  public ApiResponse<List<StandardNodeVO>> resummarize(@PathVariable Long id,
      @RequestParam(required = false) String dimension) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    return ApiResponse.ok(llmService.resummarizeSection(id, dimension, user.getTenantId(),
        user.getUserId()).stream().map(StandardNodeVO::from).toList());
  }

  /** #8 全部标已审 */
  @PostMapping("/tasks/{id}/review-done")
  @PreAuthorize("hasAuthority('ximubiaoguanli:generate')")
  @AuditLog(module = "exam", action = "standard:review-done", targetType = "exam_standard_task")
  public ApiResponse<StandardTaskVO> reviewDone(@PathVariable Long id) {
    return ApiResponse.ok(parseService.reviewDone(id, SecurityContext.currentUser().getTenantId()));
  }

  /** 删除任务 */
  @DeleteMapping("/tasks/{id}")
  @PreAuthorize("hasAuthority('ximubiaoguanli:generate')")
  @AuditLog(module = "exam", action = "standard:task-delete", targetType = "exam_standard_task")
  public ApiResponse<Void> deleteTask(@PathVariable Long id) {
    parseService.deleteTask(id, SecurityContext.currentUser().getTenantId());
    return ApiResponse.ok();
  }

  /** #9 理论细目表 Excel（缺省全范围打包 zip） */
  @GetMapping("/tasks/{id}/export/excel")
  @PreAuthorize("hasAuthority('ximubiaoguanli:export')")
  @AuditLog(module = "exam", action = "standard:export-excel", targetType = "exam_standard_task")
  public void exportExcel(@PathVariable Long id,
      @RequestParam(required = false) Integer levelCode,
      @RequestParam(required = false) String direction,
      HttpServletResponse response) throws IOException {
    writeFile(exportService.exportTheoryExcel(id, levelCode, direction,
        SecurityContext.currentUser().getTenantId()), response);
  }

  /** #10 操作技能细目表 Excel */
  @GetMapping("/tasks/{id}/export/skill-excel")
  @PreAuthorize("hasAuthority('ximubiaoguanli:export')")
  @AuditLog(module = "exam", action = "standard:export-skill-excel", targetType = "exam_standard_task")
  public void exportSkillExcel(@PathVariable Long id,
      @RequestParam(required = false) Integer levelCode,
      @RequestParam(required = false) String direction,
      HttpServletResponse response) throws IOException {
    writeFile(exportService.exportSkillExcel(id, levelCode, direction,
        SecurityContext.currentUser().getTenantId()), response);
  }

  /** #11 入库 SQL 下载（仅理论叶）；mode=create|reuse（默认 create） */
  @GetMapping("/tasks/{id}/export/sql")
  @PreAuthorize("hasAuthority('ximubiaoguanli:export')")
  @AuditLog(module = "exam", action = "standard:export-sql", targetType = "exam_standard_task")
  public void exportSql(@PathVariable Long id,
      @RequestParam(required = false, defaultValue = "create") String mode,
      HttpServletResponse response) throws IOException {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    writeFile(exportService.exportSql(id, user.getTenantId(), user.getUsername(), mode), response);
  }

  private void writeFile(ExportFile file, HttpServletResponse response) throws IOException {
    response.setContentType(file.contentType());
    String encoded = URLEncoder.encode(file.fileName(), StandardCharsets.UTF_8)
        .replace("+", "%20");
    response.setHeader("Content-Disposition",
        "attachment; filename*=UTF-8''" + encoded);
    response.setContentLength(file.content().length);
    response.getOutputStream().write(file.content());
    response.getOutputStream().flush();
  }
}
