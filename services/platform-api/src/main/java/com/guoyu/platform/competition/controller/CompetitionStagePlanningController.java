package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionStageContentBatchReq;
import com.guoyu.platform.competition.service.CompetitionStagePlanningApplicationService;
import com.guoyu.platform.competition.vo.CompetitionStageContentVO;
import com.guoyu.platform.competition.vo.CompetitionStageReadinessVO;
import com.guoyu.platform.competition.vo.CompetitionStageWorkspaceVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/competition/stages")
public class CompetitionStagePlanningController {

  private final CompetitionStagePlanningApplicationService service;

  public CompetitionStagePlanningController(CompetitionStagePlanningApplicationService service) {
    this.service = service;
  }

  @GetMapping("/{stageId}/workspace")
  @PreAuthorize("hasAuthority('competition:stage-plan:view')")
  public ApiResponse<CompetitionStageWorkspaceVO> workspace(@PathVariable Long stageId) {
    return ApiResponse.ok(service.workspace(stageId));
  }

  @GetMapping("/{stageId}/readiness")
  @PreAuthorize("hasAuthority('competition:stage-plan:view')")
  public ApiResponse<CompetitionStageReadinessVO> readiness(@PathVariable Long stageId) {
    return ApiResponse.ok(service.readiness(stageId));
  }

  @PutMapping("/{stageId}/contents")
  @PreAuthorize("hasAuthority('competition:stage-plan:manage')")
  @AuditLog(module = "competition", action = "stage-content:replace", targetType = "competition_stage_content")
  public ApiResponse<List<CompetitionStageContentVO>> replaceContents(
      @PathVariable Long stageId,
      @Valid @RequestBody CompetitionStageContentBatchReq req) {
    return ApiResponse.ok(service.replaceContents(stageId, req));
  }
}
