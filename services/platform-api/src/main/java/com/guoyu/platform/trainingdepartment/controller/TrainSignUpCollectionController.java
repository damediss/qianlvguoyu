package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpSelfQueryReq;
import com.guoyu.platform.trainingdepartment.service.TrainSignUpCollectionApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainSignUpSelfListVO;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/training")
public class TrainSignUpCollectionController {

  private final TrainSignUpCollectionApplicationService collectionService;

  public TrainSignUpCollectionController(TrainSignUpCollectionApplicationService collectionService) {
    this.collectionService = collectionService;
  }

  /** 信息收集页：报名计划列表（含企业名、人数、二维码） */
  @GetMapping("/sign-ups/collection")
  @PreAuthorize("hasAuthority('train:sign-up:view')")
  public ApiResponse<PageResponse<TrainSignUpSelfListVO>> collectionPage(
      @Valid @ModelAttribute TrainSignUpSelfQueryReq req) {
    return ApiResponse.ok(collectionService.pageSignUps(req));
  }

  /** 报名二维码（已有 CDN 文件则直接返回 URL） */
  @GetMapping("/sign-ups/{id}/qrcode")
  @PreAuthorize("hasAuthority('train:sign-up:view')")
  public ApiResponse<Map<String, Object>> qrcode(@PathVariable Long id) {
    return ApiResponse.ok(collectionService.resolveQrcode(id));
  }

  /** 导出报名人员 Excel */
  @GetMapping("/sign-up-people/export")
  @PreAuthorize("hasAuthority('train:sign-up-people:view')")
  public ResponseEntity<byte[]> exportPeople(@org.springframework.web.bind.annotation.RequestParam Long signUpId) {
    byte[] body = collectionService.exportPeopleExcel(signUpId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sign-up-people-" + signUpId + ".xlsx")
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(body);
  }
}
