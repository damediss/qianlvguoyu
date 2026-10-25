package com.guoyu.platform.textbook.controller;

import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.textbook.exporter.TextbookOutlineExporter.ExportFile;
import com.guoyu.platform.textbook.service.TextbookBookExportApplicationService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * docx 导出（方案 §6.5 E1-E4）。一期 /projects/{id}/export?format=md|xlsx 目录导出保持不变，
 * docx 是新接口，不混入旧导出器。
 */
@RestController
@RequestMapping("/textbook")
public class TextbookBookExportController {

  private final TextbookBookExportApplicationService exportService;

  public TextbookBookExportController(TextbookBookExportApplicationService exportService) {
    this.exportService = exportService;
  }

  /** E1 单节 docx（可读本节即导出当前节内容与状态）。 */
  @GetMapping("/sections/{sectionId}/export-docx")
  @PreAuthorize("hasAuthority('textbook:project:view') or hasAuthority('textbook:task:view') "
      + "or hasAuthority('textbook:review:view')")
  @AuditLog(module = "textbook", action = "section:export-docx",
      targetType = "textbook_section_content")
  public void exportSection(@PathVariable Long sectionId,
      @RequestParam(value = "mode", defaultValue = "review") String mode,
      HttpServletResponse response) throws IOException {
    writeFile(exportService.exportSection(sectionId, "review".equals(mode)), response);
  }

  /** E2 单章 docx（可读本章）。 */
  @GetMapping("/chapters/{chapterId}/export-docx")
  @PreAuthorize("hasAuthority('textbook:project:view') or hasAuthority('textbook:task:view') "
      + "or hasAuthority('textbook:review:view')")
  @AuditLog(module = "textbook", action = "chapter:export-docx", targetType = "textbook_outline")
  public void exportChapter(@PathVariable Long chapterId,
      @RequestParam(value = "mode", defaultValue = "review") String mode,
      HttpServletResponse response) throws IOException {
    writeFile(exportService.exportChapter(chapterId, "review".equals(mode)), response);
  }

  /** E3/E4 整册 docx（review 带状态标识；final 仅 done 且全部 approved 无占位）。 */
  @GetMapping("/projects/{projectId}/export-book")
  @PreAuthorize("hasAuthority('textbook:project:export')")
  @AuditLog(module = "textbook", action = "project:export-book", targetType = "textbook_project")
  public void exportBook(@PathVariable Long projectId, @RequestParam String mode,
      HttpServletResponse response) throws IOException {
    writeFile(exportService.exportBook(projectId, mode), response);
  }

  /** 照一期 TextbookProjectController.writeFile：UTF-8 filename* attachment 下载口径。 */
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
