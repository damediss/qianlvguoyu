package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainIdCardOcrReq;
import com.guoyu.platform.trainingdepartment.service.TrainAppIdCardApplicationService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** App 报名链路：证件照上传 / OCR */
@Validated
@RestController
@RequestMapping("/training/app")
public class TrainAppIdCardController {

  private final TrainAppIdCardApplicationService trainAppIdCardApplicationService;

  public TrainAppIdCardController(TrainAppIdCardApplicationService trainAppIdCardApplicationService) {
    this.trainAppIdCardApplicationService = trainAppIdCardApplicationService;
  }

  @PostMapping("/idcard-header-upload")
  @PreAuthorize("hasAnyAuthority('train:sign-up-orc:create', 'train:sign-up-people:create')")
  public ApiResponse<Map<String, Object>> uploadHeader(
      @RequestParam(required = false) Long signUpId,
      @RequestParam String idcard,
      @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(trainAppIdCardApplicationService.uploadHeader(signUpId, idcard, file));
  }

  @PostMapping("/idcard-ocr")
  @PreAuthorize("hasAnyAuthority('train:sign-up-orc:create', 'train:sign-up-people:create')")
  public ApiResponse<Map<String, Object>> ocr(@Valid @RequestBody TrainIdCardOcrReq req) {
    return ApiResponse.ok(trainAppIdCardApplicationService.ocr(req));
  }
}
