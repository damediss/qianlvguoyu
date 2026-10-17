package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.web.DownloadHeaders;
import com.guoyu.platform.competition.dto.CompetitionSimulationImportConfirmReq;
import com.guoyu.platform.competition.service.CompetitionSimulationScoreImportService;
import com.guoyu.platform.competition.vo.CompetitionSimulationImportBatchVO;
import com.guoyu.platform.competition.vo.CompetitionSimulationImportRowVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequestMapping("/competition/simulation-score-imports")
public class CompetitionSimulationScoreImportController {
  private final CompetitionSimulationScoreImportService service;

  public CompetitionSimulationScoreImportController(CompetitionSimulationScoreImportService service) {
    this.service = service;
  }

  @GetMapping("/template")
  @PreAuthorize("hasAuthority('competition:score:import')")
  public ResponseEntity<byte[]> template() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    headers.set(HttpHeaders.CONTENT_DISPOSITION,
        DownloadHeaders.contentDispositionAttachment("仿真成绩导入模板.xlsx"));
    return new ResponseEntity<>(service.template(), headers, HttpStatus.OK);
  }

  @PostMapping
  @PreAuthorize("hasAuthority('competition:score:import')")
  @AuditLog(module = "competition", action = "simulation-score:upload",
      targetType = "competition_simulation_score_import_batch", includeResult = true)
  public ResponseEntity<ApiResponse<CompetitionSimulationImportBatchVO>> upload(
      @RequestParam @NotNull Long eventId, @RequestParam @NotNull Long stageId,
      @RequestParam @NotNull Long eventWorkTypeId,
      @RequestParam(required = false) Long eventWorkTypeLevelId,
      @RequestParam @NotNull Long scoreRuleId, @RequestParam MultipartFile file) throws Exception {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CompetitionSimulationImportBatchVO batch = CompetitionSimulationImportBatchVO.from(
        service.upload(user.getTenantId(),
        user.getUserId(), eventId, stageId, eventWorkTypeId, eventWorkTypeLevelId, scoreRuleId,
        file == null ? null : file.getOriginalFilename(), file == null ? null : file.getBytes()));
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(batch));
  }

  @GetMapping("/{batchId}")
  @PreAuthorize("hasAuthority('competition:score:view')")
  public ApiResponse<CompetitionSimulationImportBatchVO> detail(@PathVariable Long batchId) {
    return ApiResponse.ok(CompetitionSimulationImportBatchVO.from(
        service.detail(SecurityContext.currentUser().getTenantId(), batchId)));
  }

  @GetMapping("/{batchId}/rows")
  @PreAuthorize("hasAuthority('competition:score:view')")
  public ApiResponse<List<CompetitionSimulationImportRowVO>> rows(
      @PathVariable Long batchId, @RequestParam(required = false) String validationStatus) {
    return ApiResponse.ok(service.rows(SecurityContext.currentUser().getTenantId(),
        batchId, validationStatus).stream().map(CompetitionSimulationImportRowVO::from).toList());
  }

  @GetMapping("/{batchId}/errors")
  @PreAuthorize("hasAuthority('competition:score:view')")
  public ResponseEntity<byte[]> errors(@PathVariable Long batchId) {
    byte[] content = service.errorWorkbook(SecurityContext.currentUser().getTenantId(), batchId);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    headers.set(HttpHeaders.CONTENT_DISPOSITION,
        DownloadHeaders.contentDispositionAttachment("仿真成绩导入错误明细.xlsx"));
    return new ResponseEntity<>(content, headers, HttpStatus.OK);
  }

  @PostMapping("/{batchId}/confirm")
  @PreAuthorize("hasAuthority('competition:score:import')")
  @AuditLog(module = "competition", action = "simulation-score:confirm",
      targetType = "competition_simulation_score_import_batch", includeResult = true)
  public ResponseEntity<ApiResponse<CompetitionSimulationImportBatchVO>> confirm(
      @PathVariable Long batchId,
      @Valid @RequestBody(required = false) CompetitionSimulationImportConfirmReq request) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CompetitionSimulationImportBatchVO batch = CompetitionSimulationImportBatchVO.from(
        service.confirm(user.getTenantId(), user.getUserId(), batchId,
            request == null ? null : request.correctionReason()));
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(batch));
  }
}
