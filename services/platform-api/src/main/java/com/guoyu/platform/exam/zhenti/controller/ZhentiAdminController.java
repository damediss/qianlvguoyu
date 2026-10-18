package com.guoyu.platform.exam.zhenti.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.exam.zhenti.dto.ZhentiIdReq;
import com.guoyu.platform.exam.zhenti.dto.ZhentiImportLogReq;
import com.guoyu.platform.exam.zhenti.dto.ZhentiQueryReq;
import com.guoyu.platform.exam.zhenti.dto.ZhentiUpdateReq;
import com.guoyu.platform.exam.zhenti.service.ZhentiAdminService;
import com.guoyu.platform.exam.zhenti.service.ZhentiImportService;
import com.guoyu.platform.exam.zhenti.vo.ZhentiAdminPageVO;
import com.guoyu.platform.exam.zhenti.vo.ZhentiImportResultVO;
import com.guoyu.platform.exam.zhenti.vo.ZhentiPreviewVO;
import com.guoyu.platform.exam.zhenti.vo.ZhentiUploadVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 管理端历年真题接口 D1-D7（方案 §3.1 / 详规 §5）。 */
@Validated
@RestController
@RequestMapping("/appzhenti/admin")
@Slf4j
public class ZhentiAdminController {

  private final ZhentiAdminService adminService;
  private final ZhentiImportService importService;

  public ZhentiAdminController(
      ZhentiAdminService adminService, ZhentiImportService importService) {
    this.adminService = adminService;
    this.importService = importService;
  }

  @PostMapping("/list")
  @PreAuthorize("hasAuthority('exam:zhenti:view')")
  public ApiResponse<ZhentiAdminPageVO> list(@Valid @RequestBody ZhentiQueryReq req) {
    return ApiResponse.ok(adminService.page(req));
  }

  @PostMapping("/upload")
  @PreAuthorize("hasAuthority('exam:zhenti:manage')")
  @AuditLog(module = "exam", action = "zhenti:upload", targetType = "exam_zhenti")
  public ApiResponse<ZhentiUploadVO> upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam("examId") Long examId,
      @RequestParam(value = "title", required = false) String title,
      @RequestParam(value = "year", required = false) String year) {
    return ApiResponse.ok(adminService.upload(file, examId, title, year));
  }

  @PostMapping("/batchImport")
  @PreAuthorize("hasAuthority('exam:zhenti:manage')")
  @AuditLog(module = "exam", action = "zhenti:batchImport", targetType = "exam_zhenti")
  public ApiResponse<ZhentiImportResultVO> batchImport(
      @RequestParam("file") MultipartFile file,
      @RequestParam("examId") Long examId) {
    return ApiResponse.ok(importService.importZip(file, examId));
  }

  @PostMapping("/update")
  @PreAuthorize("hasAuthority('exam:zhenti:manage')")
  @AuditLog(module = "exam", action = "zhenti:update", targetType = "exam_zhenti")
  public ApiResponse<ZhentiUploadVO> update(@Valid @RequestBody ZhentiUpdateReq req) {
    return ApiResponse.ok(adminService.update(req));
  }

  @PostMapping("/delete")
  @PreAuthorize("hasAuthority('exam:zhenti:manage')")
  @AuditLog(module = "exam", action = "zhenti:delete", targetType = "exam_zhenti")
  public ApiResponse<Void> delete(@Valid @RequestBody ZhentiIdReq req) {
    adminService.delete(req.id());
    return ApiResponse.ok();
  }

  @PostMapping("/importLog")
  @PreAuthorize("hasAuthority('exam:zhenti:view')")
  public ApiResponse<Object> importLog(@Valid @RequestBody ZhentiImportLogReq req) {
    return ApiResponse.ok(adminService.importLog(req));
  }

  /** D7：管理端预览签名（详规 §2.1 允许“管理端专用签名接口”）。 */
  @PostMapping("/preview")
  @PreAuthorize("hasAuthority('exam:zhenti:view')")
  public ApiResponse<ZhentiPreviewVO> preview(@Valid @RequestBody ZhentiIdReq req) {
    return ApiResponse.ok(adminService.preview(req.id()));
  }
}
