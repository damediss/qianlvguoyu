package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionTheoryPaperImportReq;
import com.guoyu.platform.competition.dto.CompetitionTheoryCompetitionPaperImportReq;
import com.guoyu.platform.competition.dto.CompetitionTheoryRequirementUpsertReq;
import com.guoyu.platform.competition.service.CompetitionTheoryPaperApplicationService;
import com.guoyu.platform.competition.vo.CompetitionTheoryCourseSourceVO;
import com.guoyu.platform.competition.vo.CompetitionTheoryPaperPreviewVO;
import com.guoyu.platform.competition.vo.CompetitionTheoryPaperSnapshotVO;
import com.guoyu.platform.competition.vo.CompetitionTheoryPaperSourceVO;
import com.guoyu.platform.competition.vo.CompetitionTheoryRequirementVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/competition")
public class CompetitionTheoryPaperController {

  private final CompetitionTheoryPaperApplicationService service;

  public CompetitionTheoryPaperController(CompetitionTheoryPaperApplicationService service) {
    this.service = service;
  }

  @GetMapping("/score-rules/{ruleId}/theory-requirement")
  @PreAuthorize("hasAuthority('competition:theory-paper:view')")
  public ApiResponse<CompetitionTheoryRequirementVO> getRequirement(
      @PathVariable Long ruleId) {
    return ApiResponse.ok(service.getRequirement(ruleId));
  }

  @PutMapping("/score-rules/{ruleId}/theory-requirement")
  @PreAuthorize("hasAuthority('competition:theory-paper:manage')")
  @AuditLog(module = "competition", action = "theory-requirement:upsert",
      targetType = "competition_theory_requirement")
  public ApiResponse<CompetitionTheoryRequirementVO> upsertRequirement(
      @PathVariable Long ruleId,
      @Valid @RequestBody CompetitionTheoryRequirementUpsertReq req) {
    return ApiResponse.ok(service.upsertRequirement(ruleId, req));
  }

  @GetMapping("/events/{eventId}/theory-exam/courses")
  @PreAuthorize("hasAuthority('competition:theory-paper:view')")
  public ApiResponse<List<CompetitionTheoryCourseSourceVO>> listCourses(
      @PathVariable Long eventId,
      @RequestParam Long eventWorkTypeId) {
    return ApiResponse.ok(service.listCourses(eventId, eventWorkTypeId));
  }

  @GetMapping("/events/{eventId}/theory-exam/source-papers")
  @PreAuthorize("hasAuthority('competition:theory-paper:view')")
  public ApiResponse<List<CompetitionTheoryPaperSourceVO>> listSourcePapers(
      @PathVariable Long eventId,
      @RequestParam Long eventWorkTypeId,
      @RequestParam Long courseId) {
    return ApiResponse.ok(service.listSourcePapers(eventId, eventWorkTypeId, courseId));
  }

  @GetMapping("/events/{eventId}/theory-exam/paper-snapshots")
  @PreAuthorize("hasAuthority('competition:theory-paper:view')")
  public ApiResponse<List<CompetitionTheoryPaperSnapshotVO>> listSnapshots(
      @PathVariable Long eventId,
      @RequestParam(required = false) Long stageId,
      @RequestParam(required = false) Long eventWorkTypeId,
      @RequestParam(required = false) Long eventWorkTypeLevelId,
      @RequestParam(required = false) Long scoreRuleId) {
    return ApiResponse.ok(
        service.listSnapshots(eventId, stageId, eventWorkTypeId, eventWorkTypeLevelId, scoreRuleId));
  }

  @GetMapping("/events/{eventId}/theory-exam/paper-snapshots/{snapshotId}/preview")
  @PreAuthorize("hasAuthority('competition:theory-paper:view')")
  public ApiResponse<CompetitionTheoryPaperPreviewVO> previewSnapshot(
      @PathVariable Long eventId,
      @PathVariable Long snapshotId) {
    return ApiResponse.ok(service.previewSnapshot(eventId, snapshotId));
  }

  @PostMapping("/events/{eventId}/theory-exam/paper-snapshots/import")
  @PreAuthorize("hasAuthority('competition:theory-paper:manage')")
  @AuditLog(module = "competition", action = "theory-paper:import",
      targetType = "competition_theory_paper_snapshot")
  public ApiResponse<List<CompetitionTheoryPaperSnapshotVO>> importPair(
      @PathVariable Long eventId,
      @Valid @RequestBody CompetitionTheoryPaperImportReq req) {
    throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
        "课程来源试卷仅保留历史只读兼容，新的竞赛试卷必须从已发布竞赛组卷快照导入");
  }

  @PostMapping("/events/{eventId}/theory-exam/paper-snapshots/import-competition")
  @PreAuthorize("hasAuthority('competition:theory-paper:manage')")
  @AuditLog(module = "competition", action = "theory-paper:import-competition-snapshot",
      targetType = "competition_theory_paper_snapshot")
  public ApiResponse<List<CompetitionTheoryPaperSnapshotVO>> importCompetitionPapers(
      @PathVariable Long eventId,
      @Valid @RequestBody CompetitionTheoryCompetitionPaperImportReq req) {
    return ApiResponse.ok(service.importCompetitionPapers(eventId, req));
  }
}
