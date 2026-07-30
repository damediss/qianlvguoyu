package com.guoyu.platform.industrial.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.industrial.dto.IndustrialTalentServiceQueryReq;
import com.guoyu.platform.industrial.dto.IndustrialTalentTrainingQueryReq;
import com.guoyu.platform.industrial.service.IndustrialTalentApplicationService;
import com.guoyu.platform.industrial.vo.IndustrialTalentServiceVO;
import com.guoyu.platform.industrial.vo.IndustrialTalentTrainingVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/industrial")
public class IndustrialTalentController {

  private final IndustrialTalentApplicationService service;

  public IndustrialTalentController(IndustrialTalentApplicationService service) {
    this.service = service;
  }

  @GetMapping("/talenttraining")
  @PreAuthorize("hasAuthority('industrial:talent-training:view')")
  public ApiResponse<PageResponse<IndustrialTalentTrainingVO>> talentTraining(
      @Valid @ModelAttribute IndustrialTalentTrainingQueryReq req
  ) {
    return ApiResponse.ok(service.pageTraining(req));
  }

  @GetMapping("/talentservice")
  @PreAuthorize("hasAuthority('industrial:talent-service:view')")
  public ApiResponse<PageResponse<IndustrialTalentServiceVO>> talentService(
      @Valid @ModelAttribute IndustrialTalentServiceQueryReq req
  ) {
    return ApiResponse.ok(service.pageService(req));
  }
}
