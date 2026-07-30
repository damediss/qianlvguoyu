package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.dto.CertificationDashboardQueryReq;
import com.guoyu.platform.certification.service.CertificationDashboardApplicationService;
import com.guoyu.platform.certification.vo.CertificationDashboardVO;
import com.guoyu.platform.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/certification/dashboard")
public class CertificationDashboardController {

  private final CertificationDashboardApplicationService service;

  public CertificationDashboardController(CertificationDashboardApplicationService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('certification.dashboard.view')")
  public ApiResponse<CertificationDashboardVO> getDashboard(
      @ModelAttribute CertificationDashboardQueryReq req
  ) {
    return ApiResponse.ok(service.getDashboard(req));
  }
}
