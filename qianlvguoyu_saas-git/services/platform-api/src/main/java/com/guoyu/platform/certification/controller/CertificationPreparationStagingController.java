package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.dto.PreparationCandidateStagingPromoteReq;
import com.guoyu.platform.certification.dto.PreparationCandidateStagingQueryReq;
import com.guoyu.platform.certification.dto.PreparationCandidateStagingSaveReq;
import com.guoyu.platform.certification.service.CertificationCandidateStagingApplicationService;
import com.guoyu.platform.certification.vo.PreparationCandidateStagingPromoteResultVO;
import com.guoyu.platform.certification.vo.PreparationCandidateStagingVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/certification/preparation")
public class CertificationPreparationStagingController {

  private final CertificationCandidateStagingApplicationService stagingService;

  public CertificationPreparationStagingController(CertificationCandidateStagingApplicationService stagingService) {
    this.stagingService = stagingService;
  }

  @GetMapping("/tasks/{taskId}/candidate-staging")
  @PreAuthorize("hasAuthority('certification.preparation.candidate.view')")
  public ApiResponse<PageResponse<PreparationCandidateStagingVO>> pageStaging(
      @PathVariable Long taskId,
      @Valid @ModelAttribute PreparationCandidateStagingQueryReq req
  ) {
    return ApiResponse.ok(stagingService.pageStaging(taskId, req));
  }

  @PostMapping("/tasks/{taskId}/candidate-staging")
  @PreAuthorize("hasAuthority('certification.preparation.candidate.edit')")
  @AuditLog(module = "certification", action = "preparation-staging:create", targetType = "certification_candidate_source_staging")
  public ApiResponse<PreparationCandidateStagingVO> createStaging(
      @PathVariable Long taskId,
      @Valid @RequestBody PreparationCandidateStagingSaveReq req
  ) {
    return ApiResponse.ok(stagingService.createStaging(taskId, req));
  }

  @PostMapping("/tasks/{taskId}/candidate-staging/promote")
  @PreAuthorize("hasAuthority('certification.preparation.candidate.import')")
  @AuditLog(module = "certification", action = "preparation-staging:promote", targetType = "certification_candidate_source_staging")
  public ApiResponse<PreparationCandidateStagingPromoteResultVO> promoteStaging(
      @PathVariable Long taskId,
      @Valid @RequestBody PreparationCandidateStagingPromoteReq req
  ) {
    return ApiResponse.ok(stagingService.promoteStaging(taskId, req));
  }
}
