package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.defense.DefenseScoringApplicationService;
import com.guoyu.platform.certification.defense.DefenseScoringModels;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/certification")
public class CertificationDefenseScoringController {

  private final DefenseScoringApplicationService service;

  public CertificationDefenseScoringController(DefenseScoringApplicationService service) {
    this.service = service;
  }

  @GetMapping("/defense-scoring/tasks/{taskId}/setup")
  @PreAuthorize("hasAnyAuthority('certification.defense.config', 'certification.preparation.task.view')")
  public ApiResponse<DefenseScoringModels.SetupView> setup(@PathVariable Long taskId) {
    return ApiResponse.ok(service.setup(taskId));
  }

  @PutMapping("/defense-scoring/tasks/{taskId}/config")
  @PreAuthorize("hasAnyAuthority('certification.defense.config', 'certification.preparation.task.view')")
  @AuditLog(module = "certification", action = "defense-scoring:config", targetType = "certification_defense_config")
  public ApiResponse<DefenseScoringModels.ConfigView> saveConfig(
      @PathVariable Long taskId,
      @Valid @RequestBody DefenseScoringModels.SaveConfigReq req
  ) {
    return ApiResponse.ok(service.saveConfig(taskId, req));
  }

  @PostMapping("/defense-scoring/tasks/{taskId}/reports/match")
  @PreAuthorize("hasAnyAuthority('certification.defense.upload', 'certification.preparation.task.view')")
  @AuditLog(module = "certification", action = "defense-scoring:match", targetType = "certification_defense_report")
  public ApiResponse<DefenseScoringModels.MatchResultView> matchReports(
      @PathVariable Long taskId,
      @Valid @RequestBody DefenseScoringModels.MatchReportsReq req
  ) {
    return ApiResponse.ok(service.matchReports(taskId, req));
  }

  @PostMapping("/defense-scoring/tasks/{taskId}/candidates/{candidateId}/report")
  @PreAuthorize("hasAnyAuthority('certification.defense.upload', 'certification.preparation.task.view')")
  @AuditLog(module = "certification", action = "defense-scoring:bind-report", targetType = "certification_defense_report")
  public ApiResponse<DefenseScoringModels.CandidateRowView> bindCandidateReport(
      @PathVariable Long taskId,
      @PathVariable Long candidateId,
      @Valid @RequestBody DefenseScoringModels.ReportFileReq req
  ) {
    return ApiResponse.ok(service.bindCandidateReport(taskId, candidateId, req));
  }

  @DeleteMapping("/defense-scoring/tasks/{taskId}/candidates/{candidateId}/report")
  @PreAuthorize("hasAnyAuthority('certification.defense.upload', 'certification.preparation.task.view')")
  @AuditLog(module = "certification", action = "defense-scoring:delete-report", targetType = "certification_defense_report")
  public ApiResponse<Void> deleteCandidateReport(
      @PathVariable Long taskId,
      @PathVariable Long candidateId
  ) {
    service.deleteCandidateReport(taskId, candidateId);
    return ApiResponse.ok();
  }

  @GetMapping("/assessor-defense/reports")
  @PreAuthorize("hasAuthority('certification.defense.review')")
  public ApiResponse<List<DefenseScoringModels.AssessorReportListItem>> myReports() {
    return ApiResponse.ok(service.myReports());
  }

  @GetMapping("/assessor-defense/reports/{reportId}")
  @PreAuthorize("hasAuthority('certification.defense.review')")
  public ApiResponse<DefenseScoringModels.AssessorReportDetail> myReport(@PathVariable Long reportId) {
    return ApiResponse.ok(service.myReport(reportId));
  }
}
