package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainIdCardOcrReq;
import com.guoyu.platform.trainingdepartment.service.TrainAppIdCardApplicationService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** App 报名链路：证件照上传 / OCR（仅需登录，不要求管理端权限） */
@Validated
@RestController
@RequestMapping("/training/app")
public class TrainAppIdCardController {

  private final TrainAppIdCardApplicationService trainAppIdCardApplicationService;

  public TrainAppIdCardController(TrainAppIdCardApplicationService trainAppIdCardApplicationService) {
    this.trainAppIdCardApplicationService = trainAppIdCardApplicationService;
  }

  @PostMapping("/idcard-header-upload")
  public ApiResponse<Map<String, Object>> uploadHeader(
      @RequestParam(required = false) Long signUpId,
      @RequestParam String idcard,
      @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(trainAppIdCardApplicationService.uploadHeader(signUpId, idcard, file));
  }

  @PostMapping("/idcard-ocr")
  public ApiResponse<Map<String, Object>> ocr(@Valid @RequestBody TrainIdCardOcrReq req) {
    return ApiResponse.ok(trainAppIdCardApplicationService.ocr(req));
  }

  @PostMapping("/file-ocr")
  public ApiResponse<Map<String, Object>> fileOcr(@RequestBody Map<String, Object> body) {
    String pic = body.get("pic") != null ? body.get("pic").toString() : "";
    String type = body.get("type") != null ? body.get("type").toString() : "";
    String idcard = body.get("idcard") != null ? body.get("idcard").toString() : "";
    return ApiResponse.ok(trainAppIdCardApplicationService.fileOcr(pic, type, idcard));
  }
}
