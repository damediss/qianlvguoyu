package com.guoyu.platform.competition.classify.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageQuery;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.web.DownloadHeaders;
import com.guoyu.platform.competition.classify.dto.CompetitionPlayerClassifyCreateReq;
import com.guoyu.platform.competition.classify.service.CompetitionPlayerClassifyApplicationService;
import com.guoyu.platform.competition.classify.vo.CompetitionPlayerClassifyTaskVO;
import com.guoyu.platform.competition.vo.CompetitionStagePaperAssignmentPlayerVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/competition/player-classify")
public class CompetitionPlayerClassifyController {

  private final CompetitionPlayerClassifyApplicationService service;

  public CompetitionPlayerClassifyController(CompetitionPlayerClassifyApplicationService service) {
    this.service = service;
  }

  @GetMapping("/stages/{stageId}/candidates")
  @PreAuthorize("hasAuthority('competition:player-classify:view')")
  public ApiResponse<List<CompetitionStagePaperAssignmentPlayerVO>> candidates(
      @PathVariable Long stageId) {
    return ApiResponse.ok(service.candidates(stageId));
  }

  @GetMapping("/stages/{stageId}/tasks")
  @PreAuthorize("hasAuthority('competition:player-classify:view')")
  public ApiResponse<PageResponse<CompetitionPlayerClassifyTaskVO>> page(
      @PathVariable Long stageId,
      @Valid @ModelAttribute PageQuery query) {
    return ApiResponse.ok(service.page(stageId, query.getPage(), query.getPageSize()));
  }

  @GetMapping("/tasks/{taskId}")
  @PreAuthorize("hasAuthority('competition:player-classify:view')")
  public ApiResponse<CompetitionPlayerClassifyTaskVO> get(@PathVariable Long taskId) {
    return ApiResponse.ok(service.get(taskId));
  }

  @GetMapping("/tasks/{taskId}/result.xlsx")
  @PreAuthorize("hasAuthority('competition:player-classify:view')")
  @AuditLog(module = "competition", action = "player-classify:export",
      targetType = "competition_player_classify_task")
  public ResponseEntity<byte[]> exportExcel(@PathVariable Long taskId) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            DownloadHeaders.contentDispositionAttachment("排序分类-任务" + taskId + ".xlsx"))
        .body(service.exportExcel(taskId));
  }

  @PostMapping("/stages/{stageId}/tasks")
  @PreAuthorize("hasAuthority('competition:player-classify:create')")
  @AuditLog(module = "competition", action = "player-classify:create",
      targetType = "competition_player_classify_task")
  public ApiResponse<CompetitionPlayerClassifyTaskVO> classify(
      @PathVariable Long stageId,
      @Valid @RequestBody CompetitionPlayerClassifyCreateReq req) {
    return ApiResponse.ok(service.classify(stageId, req));
  }

  @DeleteMapping("/tasks/{taskId}")
  @PreAuthorize("hasAuthority('competition:player-classify:delete')")
  @AuditLog(module = "competition", action = "player-classify:delete",
      targetType = "competition_player_classify_task")
  public ApiResponse<Void> delete(@PathVariable Long taskId) {
    service.delete(taskId);
    return ApiResponse.ok();
  }
}
