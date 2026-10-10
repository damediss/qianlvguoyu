package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.defense.DefenseManagementApplicationService;
import com.guoyu.platform.certification.defense.DefenseScoringApplicationService;
import com.guoyu.platform.certification.defense.DefenseScoringModels;
import com.guoyu.platform.certification.defense.DefenseScoreSheetApplicationService;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.exam.vo.ExportResultDTO;
import jakarta.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
  private final DefenseScoreSheetApplicationService scoreSheetService;
  private final DefenseManagementApplicationService managementService;

  public CertificationDefenseScoringController(
      DefenseScoringApplicationService service,
      DefenseScoreSheetApplicationService scoreSheetService,
      DefenseManagementApplicationService managementService
  ) {
    this.service = service;
    this.scoreSheetService = scoreSheetService;
    this.managementService = managementService;
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

  @GetMapping("/assessor-defense/reports/{reportId}/score-sheets")
  @PreAuthorize("hasAuthority('certification.defense.review')")
  public ApiResponse<DefenseScoringModels.ScoreSheetsView> scoreSheets(@PathVariable Long reportId) {
    return ApiResponse.ok(scoreSheetService.scoreSheets(reportId));
  }

  @PutMapping("/assessor-defense/reports/{reportId}/score-sheets/{sheetType}")
  @PreAuthorize("hasAuthority('certification.defense.review')")
  @AuditLog(module = "certification", action = "defense-scoring:save-sheet", targetType = "certification_defense_score")
  public ApiResponse<DefenseScoringModels.ScoreSheetView> saveScoreSheet(
      @PathVariable Long reportId,
      @PathVariable String sheetType,
      @Valid @RequestBody DefenseScoringModels.SaveScoreSheetReq req
  ) {
    return ApiResponse.ok(scoreSheetService.save(reportId, sheetType, req));
  }

  @PostMapping("/assessor-defense/reports/{reportId}/score-sheets/{sheetType}/submit")
  @PreAuthorize("hasAuthority('certification.defense.review')")
  @AuditLog(module = "certification", action = "defense-scoring:submit-sheet", targetType = "certification_defense_score")
  public ApiResponse<DefenseScoringModels.ScoreSheetView> submitScoreSheet(
      @PathVariable Long reportId,
      @PathVariable String sheetType,
      @Valid @RequestBody DefenseScoringModels.SubmitScoreSheetReq req
  ) {
    return ApiResponse.ok(scoreSheetService.submit(reportId, sheetType, req));
  }

  @GetMapping("/defense-scoring/tasks/{taskId}/progress")
  @PreAuthorize("hasAnyAuthority('certification.defense.progress', 'certification.preparation.task.view')")
  public ApiResponse<DefenseScoringModels.ProgressView> progress(@PathVariable Long taskId) {
    return ApiResponse.ok(managementService.progress(taskId));
  }

  @PostMapping("/defense-scoring/tasks/{taskId}/candidates/{candidateId}/absent")
  @PreAuthorize("hasAnyAuthority('certification.defense.complete', 'certification.preparation.task.view')")
  @AuditLog(module = "certification", action = "defense-scoring:mark-absent", targetType = "certification_defense_absence")
  public ApiResponse<Void> markCandidateAbsent(
      @PathVariable Long taskId,
      @PathVariable Long candidateId
  ) {
    managementService.markCandidateAbsent(taskId, candidateId);
    return ApiResponse.ok();
  }

  @PostMapping("/defense-scoring/tasks/{taskId}/complete")
  @PreAuthorize("hasAnyAuthority('certification.defense.complete', 'certification.preparation.task.view')")
  @AuditLog(module = "certification", action = "defense-scoring:complete", targetType = "certification_defense_config")
  public ApiResponse<DefenseScoringModels.CompleteView> complete(@PathVariable Long taskId) {
    return ApiResponse.ok(managementService.complete(taskId));
  }

  @GetMapping("/defense-scoring/tasks/{taskId}/summary/export")
  @PreAuthorize("hasAnyAuthority('certification.defense.export', 'certification.preparation.task.view')")
  @AuditLog(module = "certification", action = "defense-scoring:summary-export", targetType = "certification_defense_score")
  public ResponseEntity<InputStreamResource> exportSummary(@PathVariable Long taskId) throws IOException {
    ExportResultDTO result = managementService.exportSummary(taskId);
    InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(result.data()));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(result.filename(), StandardCharsets.UTF_8).build().toString())
        .contentType(result.filename() != null && result.filename().endsWith(".zip")
            ? MediaType.parseMediaType("application/zip")
            : MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        .body(resource);
  }
}
