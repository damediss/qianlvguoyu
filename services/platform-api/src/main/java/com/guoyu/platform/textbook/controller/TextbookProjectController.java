package com.guoyu.platform.textbook.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.textbook.dto.OutlineNodeCreateReq;
import com.guoyu.platform.textbook.dto.OutlineNodeUpdateReq;
import com.guoyu.platform.textbook.dto.ProjectCreateReq;
import com.guoyu.platform.textbook.dto.ProjectQueryReq;
import com.guoyu.platform.textbook.dto.ProjectUpdateReq;
import com.guoyu.platform.textbook.exporter.TextbookOutlineExporter.ExportFile;
import com.guoyu.platform.textbook.service.TextbookOutlineApplicationService;
import com.guoyu.platform.textbook.service.TextbookProjectApplicationService;
import com.guoyu.platform.textbook.vo.OutlineNodeVO;
import com.guoyu.platform.textbook.vo.OutlineSummaryVO;
import com.guoyu.platform.textbook.vo.ParsePreviewVO;
import com.guoyu.platform.textbook.vo.TextbookProjectVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * 职业标准驱动教材生成（一期 M1/M2）：/api/textbook/**，权限码 textbook:project:*。
 * 多租户过滤由 MyBatis-Plus 插件自动拼接（AGENTS.md 红线），本类不手写 tenant 条件。
 */
@RestController
@RequestMapping("/textbook")
@Slf4j
public class TextbookProjectController {

  private final TextbookProjectApplicationService projectService;
  private final TextbookOutlineApplicationService outlineService;

  public TextbookProjectController(TextbookProjectApplicationService projectService,
      TextbookOutlineApplicationService outlineService) {
    this.projectService = projectService;
    this.outlineService = outlineService;
  }

  /** #1 同步解析预览（不落库），供新建向导回填 */
  @PostMapping(value = "/projects/parse-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('textbook:project:manage')")
  public ApiResponse<ParsePreviewVO> parsePreview(@RequestParam("file") MultipartFile file)
      throws IOException {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件为空");
    }
    return ApiResponse.ok(projectService.parsePreview(file.getBytes(), file.getOriginalFilename()));
  }

  /**
   * #2 创建工程并同步完成 解析→方向过滤→骨架→配比（失败整体回滚）——multipart 形态（前端默认）：
   * file=原始 docx + direction + levelCode + functionNos（逗号分隔串，如 "1,2,12"）
   * + trainingHours? + wordsPerHour?；后端代存文件（file 模块）后走统一创建链路。
   */
  @PostMapping(value = "/projects", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('textbook:project:manage')")
  @AuditLog(module = "textbook", action = "project:create", targetType = "textbook_project")
  public ApiResponse<TextbookProjectVO> createMultipart(
      @RequestParam("file") MultipartFile file,
      @RequestParam("direction") String direction,
      @RequestParam("levelCode") Integer levelCode,
      @RequestParam(value = "functionNos", required = false) String functionNos,
      @RequestParam(value = "trainingHours", required = false) Integer trainingHours,
      @RequestParam(value = "wordsPerHour", required = false) Integer wordsPerHour)
      throws IOException {
    if (file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件为空");
    }
    ProjectCreateReq multipartReq = multipartReq(direction, levelCode, functionNos,
        trainingHours, wordsPerHour);
    return ApiResponse.ok(projectService.createFromUpload(
        file.getBytes(), file.getOriginalFilename(), multipartReq));
  }

  /** #2 创建（JSON 形态，兼容 file 模块预传）：fileId + direction + levelCode + functionNos[] */
  @PostMapping(value = "/projects", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('textbook:project:manage')")
  @AuditLog(module = "textbook", action = "project:create", targetType = "textbook_project")
  public ApiResponse<TextbookProjectVO> createJson(@Valid @RequestBody ProjectCreateReq req) {
    return ApiResponse.ok(projectService.create(req));
  }

  /** multipart 字段手工校验（@RequestParam 不走 Bean Validation），拼装为统一创建请求 */
  private ProjectCreateReq multipartReq(String direction, Integer levelCode, String functionNos,
      Integer trainingHours, Integer wordsPerHour) {
    if (direction == null || direction.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "direction 不能为空");
    }
    if (levelCode == null || levelCode < 1 || levelCode > 5) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "levelCode 为 5=五级 … 1=一级");
    }
    List<Integer> nos = null;
    if (functionNos != null && !functionNos.isBlank()) {
      nos = new java.util.ArrayList<>();
      for (String part : functionNos.split("[,，、\\s]+")) {
        if (part.isEmpty()) {
          continue;
        }
        try {
          int no = Integer.parseInt(part.trim());
          if (no < 1 || no > 99) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "functionNos 越界：" + part);
          }
          nos.add(no);
        } catch (NumberFormatException e) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "functionNos 含非数字项：" + part);
        }
      }
    }
    if (trainingHours != null && trainingHours < 1) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "trainingHours 至少 1");
    }
    if (wordsPerHour != null && wordsPerHour < 100) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "wordsPerHour 至少 100");
    }
    return new ProjectCreateReq(null, direction.trim(), levelCode, nos, trainingHours,
        wordsPerHour);
  }

  /** #3 分页列表 */
  @GetMapping("/projects")
  @PreAuthorize("hasAuthority('textbook:project:view')")
  public ApiResponse<PageResponse<TextbookProjectVO>> page(@Valid ProjectQueryReq req) {
    return ApiResponse.ok(projectService.page(req));
  }

  /** #4 详情 + 统计（章/节/点数、合计校验） */
  @GetMapping("/projects/{id}")
  @PreAuthorize("hasAuthority('textbook:project:view')")
  public ApiResponse<TextbookProjectVO> detail(@PathVariable Long id) {
    return ApiResponse.ok(projectService.detail(id));
  }

  /** #5 更新：改名称/学时/系数/功能映射/状态流转；结构变化自动重建骨架（保留人工字数） */
  @PutMapping("/projects/{id}")
  @PreAuthorize("hasAuthority('textbook:project:manage')")
  @AuditLog(module = "textbook", action = "project:update", targetType = "textbook_project")
  public ApiResponse<TextbookProjectVO> update(@PathVariable Long id,
      @Valid @RequestBody ProjectUpdateReq req) {
    return ApiResponse.ok(projectService.update(id, req));
  }

  /** #6 删除工程（outline 节点按 project_id 级联逻辑删，同事务） */
  @DeleteMapping("/projects/{id}")
  @PreAuthorize("hasAuthority('textbook:project:manage')")
  @AuditLog(module = "textbook", action = "project:delete", targetType = "textbook_project")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    projectService.delete(id);
    return ApiResponse.ok();
  }

  /**
   * #7 手动重建骨架 + 配比（未人工调整的字数将被覆盖，前端二次确认）。
   * 响应即最新合计校验 {chapterSum,totalWords,balanced,unbalancedChapters[]}。
   */
  @PostMapping("/projects/{id}/rebuild")
  @PreAuthorize("hasAuthority('textbook:project:manage')")
  @AuditLog(module = "textbook", action = "project:rebuild", targetType = "textbook_project")
  public ApiResponse<OutlineSummaryVO> rebuild(@PathVariable Long id) {
    return ApiResponse.ok(projectService.rebuild(id));
  }

  /** #8 大纲扁平列表（按 parent_id/sort_no 有序，前端自组树） */
  @GetMapping("/projects/{id}/outline")
  @PreAuthorize("hasAuthority('textbook:project:view')")
  public ApiResponse<List<OutlineNodeVO>> outline(@PathVariable Long id) {
    return ApiResponse.ok(outlineService.outline(id));
  }

  /** #9 改节点 title/target_words（置 words_manual=1）/sort_no；返回更新后的合计校验 */
  @PutMapping("/outline/nodes/{nodeId}")
  @PreAuthorize("hasAuthority('textbook:project:manage')")
  @AuditLog(module = "textbook", action = "outline:node-update", targetType = "textbook_outline")
  public ApiResponse<OutlineSummaryVO> updateNode(@PathVariable Long nodeId,
      @Valid @RequestBody OutlineNodeUpdateReq req) {
    return ApiResponse.ok(outlineService.updateNode(nodeId, req));
  }

  /** #10 人工新增子节点（父为章→节；父为节→点 dimension 必填；source_no 空） */
  @PostMapping("/outline/nodes")
  @PreAuthorize("hasAuthority('textbook:project:manage')")
  @AuditLog(module = "textbook", action = "outline:node-create", targetType = "textbook_outline")
  public ApiResponse<OutlineNodeVO> createNode(@Valid @RequestBody OutlineNodeCreateReq req) {
    return ApiResponse.ok(outlineService.createNode(req));
  }

  /** #11 软删节点及其子孙（同事务） */
  @DeleteMapping("/outline/nodes/{nodeId}")
  @PreAuthorize("hasAuthority('textbook:project:manage')")
  @AuditLog(module = "textbook", action = "outline:node-delete", targetType = "textbook_outline")
  public ApiResponse<Void> deleteNode(@PathVariable Long nodeId) {
    outlineService.deleteNode(nodeId);
    return ApiResponse.ok();
  }

  /** #12 目录导出（Markdown / Excel，附配比依据列；不平衡标注不阻塞） */
  @GetMapping("/projects/{id}/export")
  @PreAuthorize("hasAuthority('textbook:project:export')")
  @AuditLog(module = "textbook", action = "project:export", targetType = "textbook_project")
  public void export(@PathVariable Long id, @RequestParam String format,
      HttpServletResponse response) throws IOException {
    writeFile(projectService.export(id, format), response);
  }

  /** 照 StandardParseController.writeFile：UTF-8 文件名编码 + attachment 下载 */
  private void writeFile(ExportFile file, HttpServletResponse response) throws IOException {
    response.setContentType(file.contentType());
    String encoded = URLEncoder.encode(file.fileName(), StandardCharsets.UTF_8)
        .replace("+", "%20");
    response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
    response.setContentLength(file.content().length);
    response.getOutputStream().write(file.content());
    response.getOutputStream().flush();
  }
}
