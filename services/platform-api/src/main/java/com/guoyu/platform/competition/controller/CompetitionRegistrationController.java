package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionRegistrationAuditRequest;
import com.guoyu.platform.competition.dto.CompetitionRegistrationQueryReq;
import com.guoyu.platform.competition.service.CompetitionRegistrationApplicationService;
import com.guoyu.platform.competition.vo.CompetitionRegistrationAuditResultVO;
import com.guoyu.platform.competition.vo.CompetitionRegistrationVO;
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
@RequestMapping("/competition/registrations")
public class CompetitionRegistrationController {

  private final CompetitionRegistrationApplicationService service;

  public CompetitionRegistrationController(CompetitionRegistrationApplicationService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('competition:registration:view')")
  public ApiResponse<PageResponse<CompetitionRegistrationVO>> page(
      @Valid @ModelAttribute CompetitionRegistrationQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @PostMapping("/{registrationId}/audit")
  @PreAuthorize("hasAuthority('competition:registration:audit')")
  @AuditLog(
      module = "competition",
      action = "registration:audit",
      targetType = "competition_registration",
      includeResult = true)
  public ApiResponse<CompetitionRegistrationAuditResultVO> audit(
      @PathVariable Long registrationId,
      @Valid @RequestBody CompetitionRegistrationAuditRequest req) {
    return ApiResponse.ok(service.audit(registrationId, req));
  }
}
