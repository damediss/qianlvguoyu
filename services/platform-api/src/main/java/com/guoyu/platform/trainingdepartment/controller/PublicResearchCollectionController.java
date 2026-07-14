package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchPersonCreateReq;
import com.guoyu.platform.trainingdepartment.service.TrainResearchCollectionApplicationService;
import com.guoyu.platform.trainingdepartment.vo.PublicResearchCollectionSubmitResultVO;
import com.guoyu.platform.trainingdepartment.vo.PublicResearchCollectionVO;
import com.guoyu.platform.trainingdepartment.vo.TrainResearchPersonVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业调研人员采集 - 公开 H5 接口（扫码进入，无需登录）。
 */
@RestController
@RequestMapping("/training/public/research-collection")
@RequiredArgsConstructor
@Validated
public class PublicResearchCollectionController {

  private final TrainResearchCollectionApplicationService service;

  @GetMapping("/{token}")
  public ApiResponse<PublicResearchCollectionVO> detail(@PathVariable String token) {
    return ApiResponse.ok(service.getPublicCollection(token));
  }

  @GetMapping("/{token}/mine")
  public ApiResponse<TrainResearchPersonVO> findMine(
      @PathVariable String token,
      @RequestParam @NotBlank @Pattern(regexp = "^\\d{17}[\\dXx]$") String idCardNumber) {
    return ApiResponse.ok(service.findPublicPerson(token, idCardNumber));
  }

  @PostMapping("/{token}/submit")
  public ApiResponse<PublicResearchCollectionSubmitResultVO> submit(
      @PathVariable String token,
      @Valid @RequestBody TrainResearchPersonCreateReq req) {
    return ApiResponse.ok(service.submitPublicPerson(token, req));
  }
}
