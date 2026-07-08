package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.dto.CertificationMcpTaskQueryReq;
import com.guoyu.platform.certification.service.CertificationMcpQueryApplicationService;
import com.guoyu.platform.certification.vo.CertificationMcpCollectionProgressVO;
import com.guoyu.platform.certification.vo.CertificationMcpPracticalScoreProgressVO;
import com.guoyu.platform.certification.vo.CertificationMcpTaskItemVO;
import com.guoyu.platform.certification.vo.CertificationMcpTaskOverviewVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/certification/mcp")
public class CertificationMcpController {

  private final CertificationMcpQueryApplicationService service;

  public CertificationMcpController(CertificationMcpQueryApplicationService service) {
    this.service = service;
  }

  @GetMapping("/tasks")
  @PreAuthorize("hasAuthority('certification.dashboard.view')")
  public ApiResponse<PageResponse<CertificationMcpTaskItemVO>> searchTasks(
      @Valid @ModelAttribute CertificationMcpTaskQueryReq req
  ) {
    return ApiResponse.ok(service.searchTasks(req));
  }

  @GetMapping("/tasks/{taskId}/overview")
  @PreAuthorize("hasAuthority('certification.dashboard.view')")
  public ApiResponse<CertificationMcpTaskOverviewVO> getTaskOverview(@PathVariable Long taskId) {
    return ApiResponse.ok(service.getTaskOverview(taskId));
  }

  @GetMapping("/tasks/{taskId}/collection-progress")
  @PreAuthorize("hasAuthority('certification.dashboard.view')")
  public ApiResponse<CertificationMcpCollectionProgressVO> getCollectionProgress(@PathVariable Long taskId) {
    return ApiResponse.ok(service.getCollectionProgress(taskId));
  }

  @GetMapping("/tasks/{taskId}/practical-score-progress")
  @PreAuthorize("hasAuthority('certification.dashboard.view')")
  public ApiResponse<CertificationMcpPracticalScoreProgressVO> getPracticalScoreProgress(
      @PathVariable Long taskId
  ) {
    return ApiResponse.ok(service.getPracticalScoreProgress(taskId));
  }
}
