package com.guoyu.platform.exam.zhenti.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.exam.zhenti.dto.ZhentiIdReq;
import com.guoyu.platform.exam.zhenti.dto.ZhentiImportLogReq;
import com.guoyu.platform.exam.zhenti.dto.ZhentiQueryReq;
import com.guoyu.platform.exam.zhenti.dto.ZhentiScopeBatchSaveReq;
import com.guoyu.platform.exam.zhenti.dto.ZhentiScopeInfoReq;
import com.guoyu.platform.exam.zhenti.dto.ZhentiUpdateReq;
import com.guoyu.platform.exam.zhenti.service.ZhentiAdminService;
import com.guoyu.platform.exam.zhenti.service.ZhentiImportService;
import com.guoyu.platform.exam.zhenti.service.ZhentiScopeAdminService;
import com.guoyu.platform.exam.zhenti.vo.ZhentiAdminPageVO;
import com.guoyu.platform.exam.zhenti.vo.ZhentiImportResultVO;
import com.guoyu.platform.exam.zhenti.vo.ZhentiPreviewVO;
import com.guoyu.platform.exam.zhenti.vo.ZhentiScopeInfoVO;
import com.guoyu.platform.exam.zhenti.vo.ZhentiScopePlansVO;
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
  private final ZhentiScopeAdminService scopeAdminService;

  public ZhentiAdminController(
      ZhentiAdminService adminService,
      ZhentiImportService importService,
      ZhentiScopeAdminService scopeAdminService) {
    this.adminService = adminService;
    this.importService = importService;
    this.scopeAdminService = scopeAdminService;
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
      @RequestParam(value = "year", required = false) String year,
      @RequestParam(value = "scopeWorkTypeId", required = false) Long scopeWorkTypeId,
      @RequestParam(value = "scopeLevel", required = false) Integer scopeLevel) {
    return ApiResponse.ok(
        adminService.upload(file, examId, title, year, scopeWorkTypeId, scopeLevel));
  }

  @PostMapping("/batchImport")
  @PreAuthorize("hasAuthority('exam:zhenti:manage')")
  @AuditLog(module = "exam", action = "zhenti:batchImport", targetType = "exam_zhenti")
  public ApiResponse<ZhentiImportResultVO> batchImport(
      @RequestParam("file") MultipartFile file,
      @RequestParam("examId") Long examId,
      @RequestParam(value = "scopeWorkTypeId", required = false) Long scopeWorkTypeId,
      @RequestParam(value = "scopeLevel", required = false) Integer scopeLevel) {
    return ApiResponse.ok(importService.importZip(file, examId, scopeWorkTypeId, scopeLevel));
  }

  /** 共享组信息：提示条与上传弹窗的选择项（方案 v2.1 §7）。 */
  @PostMapping("/scopeInfo")
  @PreAuthorize("hasAuthority('exam:zhenti:view')")
  public ApiResponse<ZhentiScopeInfoVO> scopeInfo(@Valid @RequestBody ZhentiScopeInfoReq req) {
    return ApiResponse.ok(scopeAdminService.scopeInfo(req));
  }

  /** 「真题共享范围」页：计划绑定列表（方案 v2.1 §7，Q5 已拍板长期维护）。 */
  @PostMapping("/scopePlans")
  @PreAuthorize("hasAuthority('exam:zhenti:view')")
  public ApiResponse<ZhentiScopePlansVO> scopePlans(
      @RequestParam(value = "page", required = false) Long page,
      @RequestParam(value = "pageSize", required = false) Long pageSize,
      @RequestParam(value = "keyword", required = false) String keyword,
      @RequestParam(value = "filter", required = false) String filter) {
    return ApiResponse.ok(scopeAdminService.scopePlans(page, pageSize, keyword, filter));
  }

  /** 「真题共享范围」页：批量保存课程绑定（编辑模型=课程绑定，重推导工种/等级）。 */
  @PostMapping("/scopeBatchSave")
  @PreAuthorize("hasAuthority('exam:zhenti:manage')")
  @AuditLog(module = "exam", action = "zhenti:scopeBatchSave", targetType = "train_exam_work_type_list")
  public ApiResponse<Void> scopeBatchSave(@Valid @RequestBody ZhentiScopeBatchSaveReq req) {
    scopeAdminService.scopeBatchSave(req);
    return ApiResponse.ok();
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
