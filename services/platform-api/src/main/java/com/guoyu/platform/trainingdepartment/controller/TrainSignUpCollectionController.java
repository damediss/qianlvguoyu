package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.SignUpDefaultTemplateExportCheckReq;
import com.guoyu.platform.trainingdepartment.dto.SignUpDefaultTemplateExportReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpSelfQueryReq;
import com.guoyu.platform.trainingdepartment.service.TrainSignUpCollectionApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainSignUpDefaultTemplateExportService;
import com.guoyu.platform.trainingdepartment.vo.SignUpDefaultTemplateExportCheckVO;
import com.guoyu.platform.trainingdepartment.vo.SignUpDefaultTemplateExportStartVO;
import com.guoyu.platform.trainingdepartment.vo.TrainSignUpSelfListVO;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/training")
public class TrainSignUpCollectionController {

  private final TrainSignUpCollectionApplicationService collectionService;
  private final TrainSignUpDefaultTemplateExportService defaultTemplateExportService;

  public TrainSignUpCollectionController(
      TrainSignUpCollectionApplicationService collectionService,
      TrainSignUpDefaultTemplateExportService defaultTemplateExportService) {
    this.collectionService = collectionService;
    this.defaultTemplateExportService = defaultTemplateExportService;
  }

  /** 信息收集页：报名计划列表（含企业名、人数、二维码） */
  @GetMapping("/sign-ups/collection")
  @PreAuthorize("hasAuthority('train:sign-up:view')")
  public ApiResponse<PageResponse<TrainSignUpSelfListVO>> collectionPage(
      @Valid @ModelAttribute TrainSignUpSelfQueryReq req) {
    return ApiResponse.ok(collectionService.pageSignUps(req));
  }

  /** 报名二维码；generate=true 时生成微信小程序码（对齐 /wxmini/signup-self-qrcode） */
  @GetMapping("/sign-ups/{id}/qrcode")
  @PreAuthorize("hasAuthority('train:sign-up:view')")
  public ApiResponse<Map<String, Object>> qrcode(
      @PathVariable Long id,
      @RequestParam(defaultValue = "false") boolean generate) {
    if (generate) {
      return ApiResponse.ok(collectionService.generateQrcode(id));
    }
    return ApiResponse.ok(collectionService.resolveQrcode(id));
  }

  /** @deprecated 请使用 GET /sign-ups/{id}/qrcode?generate=true */
  @PostMapping("/sign-ups/{id}/qrcode/generate")
  @PreAuthorize("hasAuthority('train:sign-up:view')")
  public ApiResponse<Map<String, Object>> generateQrcode(@PathVariable Long id) {
    return ApiResponse.ok(collectionService.generateQrcode(id));
  }

  /** 导出报名人员汇总表（对齐桌面《报名人员汇总表模板》A–Y 25 列） */
  @GetMapping("/sign-up-people/export")
  @PreAuthorize("hasAuthority('train:sign-up-people:view')")
  public ResponseEntity<byte[]> exportPeople(
      @RequestParam Long signUpId,
      @RequestParam(required = false) String ids) {
    var export = collectionService.exportPeopleExcel(signUpId, ids);
    byte[] body = export.body();
    String asciiName = "sign-up-people-" + signUpId + ".xlsx";
    String utf8Name = URLEncoder.encode(export.fileName(), StandardCharsets.UTF_8)
        .replace("+", "%20");
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + asciiName + "\"; filename*=UTF-8''" + utf8Name)
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(body);
  }

  /** 默认模板导出：创建异步任务（对齐 peopleselfexport） */
  @PostMapping("/sign-up-people/export/default-template")
  @PreAuthorize("hasAuthority('train:sign-up-people:view')")
  public ApiResponse<SignUpDefaultTemplateExportStartVO> startDefaultTemplateExport(
      @Valid @RequestBody SignUpDefaultTemplateExportReq req) {
    return ApiResponse.ok(defaultTemplateExportService.startExport(req));
  }

  /** 默认模板导出：查询进度（对齐 peopleselfexportcheck） */
  @PostMapping("/sign-up-people/export/default-template/check")
  @PreAuthorize("hasAuthority('train:sign-up-people:view')")
  public ApiResponse<SignUpDefaultTemplateExportCheckVO> checkDefaultTemplateExport(
      @Valid @RequestBody SignUpDefaultTemplateExportCheckReq req) {
    return ApiResponse.ok(defaultTemplateExportService.checkExport(req));
  }
}
