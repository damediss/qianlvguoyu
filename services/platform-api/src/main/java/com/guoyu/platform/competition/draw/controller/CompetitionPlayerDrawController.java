package com.guoyu.platform.competition.draw.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageQuery;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.draw.dto.CompetitionPlayerDrawTaskSelectionReq;
import com.guoyu.platform.competition.draw.service.CompetitionPlayerDrawApplicationService;
import com.guoyu.platform.competition.draw.vo.CompetitionPlayerDrawTaskVO;
import com.guoyu.platform.competition.vo.CompetitionStagePaperAssignmentPlayerVO;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/competition/player-draw")
public class CompetitionPlayerDrawController {

  private final CompetitionPlayerDrawApplicationService service;

  public CompetitionPlayerDrawController(CompetitionPlayerDrawApplicationService service) {
    this.service = service;
  }

  @GetMapping("/stages/{stageId}/candidates")
  @PreAuthorize("hasAuthority('competition:player-draw:view')")
  public ApiResponse<List<CompetitionStagePaperAssignmentPlayerVO>> candidates(
      @PathVariable Long stageId) {
    return ApiResponse.ok(service.candidates(stageId));
  }

  @GetMapping("/stages/{stageId}/tasks")
  @PreAuthorize("hasAuthority('competition:player-draw:view')")
  public ApiResponse<PageResponse<CompetitionPlayerDrawTaskVO>> page(
      @PathVariable Long stageId,
      @Valid @ModelAttribute PageQuery query) {
    return ApiResponse.ok(service.page(stageId, query.getPage(), query.getPageSize()));
  }

  @PostMapping("/stages/{stageId}/tasks")
  @PreAuthorize("hasAuthority('competition:player-draw:create')")
  @AuditLog(module = "competition", action = "player-draw:create",
      targetType = "competition_player_draw_task")
  public ApiResponse<CompetitionPlayerDrawTaskVO> create(@PathVariable Long stageId) {
    return ApiResponse.ok(service.create(stageId));
  }

  @DeleteMapping("/tasks/{taskId}")
  @PreAuthorize("hasAuthority('competition:player-draw:delete')")
  @AuditLog(module = "competition", action = "player-draw:delete",
      targetType = "competition_player_draw_task")
  public ApiResponse<Void> delete(@PathVariable Long taskId) {
    service.delete(taskId);
    return ApiResponse.ok();
  }

  @PutMapping("/tasks/{taskId}/selection")
  @PreAuthorize("hasAuthority('competition:player-draw:create')")
  @AuditLog(module = "competition", action = "player-draw:select",
      targetType = "competition_player_draw_task")
  public ApiResponse<CompetitionPlayerDrawTaskVO> selectContestants(
      @PathVariable Long taskId,
      @Valid @RequestBody CompetitionPlayerDrawTaskSelectionReq req) {
    return ApiResponse.ok(service.selectContestants(taskId, req));
  }

  @PostMapping("/tasks/{taskId}/draw-order")
  @PreAuthorize("hasAuthority('competition:player-draw:draw')")
  @AuditLog(module = "competition", action = "player-draw:order",
      targetType = "competition_player_draw_task")
  public ApiResponse<CompetitionPlayerDrawTaskVO> generateDrawOrder(@PathVariable Long taskId) {
    return ApiResponse.ok(service.generateDrawOrder(taskId));
  }

  @PostMapping("/tasks/{taskId}/draw-next")
  @PreAuthorize("hasAuthority('competition:player-draw:draw')")
  @AuditLog(module = "competition", action = "player-draw:draw",
      targetType = "competition_player_draw_task")
  public ApiResponse<CompetitionPlayerDrawTaskVO> drawNext(@PathVariable Long taskId) {
    return ApiResponse.ok(service.drawNext(taskId));
  }

  @GetMapping("/tasks/{taskId}")
  @PreAuthorize("hasAuthority('competition:player-draw:view')")
  public ApiResponse<CompetitionPlayerDrawTaskVO> get(@PathVariable Long taskId) {
    return ApiResponse.ok(service.get(taskId));
  }

  @GetMapping("/tasks/{taskId}/roster.xlsx")
  @PreAuthorize("hasAuthority('competition:player-draw:export')")
  @AuditLog(module = "competition", action = "player-draw:export",
      targetType = "competition_player_draw_task")
  public ResponseEntity<byte[]> exportExcel(@PathVariable Long taskId) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
            .filename("抽签出场名单-任务" + taskId + ".xlsx", StandardCharsets.UTF_8)
            .build().toString())
        .body(service.exportExcel(taskId));
  }
}
