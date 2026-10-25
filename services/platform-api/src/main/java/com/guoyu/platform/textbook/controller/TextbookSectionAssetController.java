package com.guoyu.platform.textbook.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.web.DownloadHeaders;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.textbook.service.TextbookSectionAssetApplicationService;
import com.guoyu.platform.textbook.service.TextbookSectionAssetApplicationService.AssetUploadResult;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 节正文图片（方案 §6.4 A1-A3）。A2 是受保护教材资产的唯一在线读取入口。
 */
@RestController
@RequestMapping("/textbook")
public class TextbookSectionAssetController {

  private final TextbookSectionAssetApplicationService assetService;
  private final FileApplicationService fileApplicationService;

  public TextbookSectionAssetController(
      TextbookSectionAssetApplicationService assetService,
      FileApplicationService fileApplicationService) {
    this.assetService = assetService;
    this.fileApplicationService = fileApplicationService;
  }

  /** A1 上传（当前主笔；untouched/draft/returned；写入 protected/textbook/**）。 */
  @PostMapping("/sections/{sectionId}/assets")
  @PreAuthorize("hasAuthority('textbook:content:write')")
  @AuditLog(module = "textbook", action = "section:asset-upload",
      targetType = "textbook_section_asset")
  public ApiResponse<AssetUploadResult> upload(@PathVariable Long sectionId,
      @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(assetService.upload(sectionId, file));
  }

  /** A2 inline 下载（节读取授权后；Cache-Control: private, no-store）。 */
  @GetMapping("/sections/{sectionId}/assets/{assetId}/content")
  @PreAuthorize("hasAuthority('textbook:project:view') or hasAuthority('textbook:task:view') "
      + "or hasAuthority('textbook:review:view')")
  public ResponseEntity<Resource> downloadContent(@PathVariable Long sectionId,
      @PathVariable Long assetId) {
    return assetService.downloadContent(sectionId, assetId);
  }

  /** A3 删除（当前主笔；锁内确认当前正文与全部快照均未引用）。 */
  @DeleteMapping("/sections/{sectionId}/assets/{assetId}")
  @PreAuthorize("hasAuthority('textbook:content:write')")
  @AuditLog(module = "textbook", action = "section:asset-delete",
      targetType = "textbook_section_asset")
  public ApiResponse<Void> delete(@PathVariable Long sectionId, @PathVariable Long assetId,
      @RequestParam(value = "baseVersion", required = false) Integer baseVersion) {
    assetService.delete(sectionId, assetId, baseVersion);
    return ApiResponse.ok();
  }
}
