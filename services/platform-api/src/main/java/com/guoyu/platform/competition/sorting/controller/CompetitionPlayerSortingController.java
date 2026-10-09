package com.guoyu.platform.competition.sorting.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageQuery;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.sorting.dto.CompetitionPlayerSortingGroupsReq;
import com.guoyu.platform.competition.sorting.dto.CompetitionPlayerSortingRolesReq;
import com.guoyu.platform.competition.sorting.dto.CompetitionPlayerSortingTaskCreateReq;
import com.guoyu.platform.competition.sorting.dto.CompetitionPlayerSortingTaskSelectionReq;
import com.guoyu.platform.competition.sorting.dto.CompetitionPlayerSortingWorkstationsReq;
import com.guoyu.platform.competition.sorting.service.CompetitionPlayerSortingApplicationService;
import com.guoyu.platform.competition.sorting.vo.CompetitionPlayerSortingTaskVO;
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
@RequestMapping("/competition/player-sorting")
public class CompetitionPlayerSortingController {

  private final CompetitionPlayerSortingApplicationService service;

  public CompetitionPlayerSortingController(CompetitionPlayerSortingApplicationService service) {
    this.service = service;
  }

  @GetMapping("/stages/{stageId}/candidates")
  @PreAuthorize("hasAuthority('competition:player-sorting:view')")
  public ApiResponse<List<CompetitionStagePaperAssignmentPlayerVO>> candidates(
      @PathVariable Long stageId) {
    return ApiResponse.ok(service.candidates(stageId));
  }

  @GetMapping("/stages/{stageId}/tasks")
  @PreAuthorize("hasAuthority('competition:player-sorting:view')")
  public ApiResponse<PageResponse<CompetitionPlayerSortingTaskVO>> page(
      @PathVariable Long stageId,
      @Valid @ModelAttribute PageQuery query) {
    return ApiResponse.ok(service.page(stageId, query.getPage(), query.getPageSize()));
  }

  @PostMapping("/stages/{stageId}/tasks")
  @PreAuthorize("hasAuthority('competition:player-sorting:create')")
  @AuditLog(module = "competition", action = "player-sorting:create",
      targetType = "competition_practical_sorting_task")
  public ApiResponse<CompetitionPlayerSortingTaskVO> create(
      @PathVariable Long stageId,
      @Valid @RequestBody CompetitionPlayerSortingTaskCreateReq req) {
    return ApiResponse.ok(service.create(stageId, req));
  }

  @DeleteMapping("/tasks/{taskId}")
  @PreAuthorize("hasAuthority('competition:player-sorting:delete')")
  @AuditLog(module = "competition", action = "player-sorting:delete",
      targetType = "competition_practical_sorting_task")
  public ApiResponse<Void> delete(@PathVariable Long taskId) {
    service.delete(taskId);
    return ApiResponse.ok();
  }

  @PutMapping("/tasks/{taskId}/selection")
  @PreAuthorize("hasAuthority('competition:player-sorting:create')")
  @AuditLog(module = "competition", action = "player-sorting:select",
      targetType = "competition_practical_sorting_task")
  public ApiResponse<CompetitionPlayerSortingTaskVO> selectContestants(
      @PathVariable Long taskId,
      @Valid @RequestBody CompetitionPlayerSortingTaskSelectionReq req) {
    return ApiResponse.ok(service.selectContestants(taskId, req));
  }

  @PutMapping("/tasks/{taskId}/groups")
  @PreAuthorize("hasAuthority('competition:player-sorting:create')")
  @AuditLog(module = "competition", action = "player-sorting:groups",
      targetType = "competition_practical_sorting_task")
  public ApiResponse<CompetitionPlayerSortingTaskVO> saveGroups(
      @PathVariable Long taskId,
      @Valid @RequestBody CompetitionPlayerSortingGroupsReq req) {
    return ApiResponse.ok(service.saveGroups(taskId, req));
  }

  @PutMapping("/tasks/{taskId}/roles")
  @PreAuthorize("hasAuthority('competition:player-sorting:create')")
  @AuditLog(module = "competition", action = "player-sorting:roles",
      targetType = "competition_practical_sorting_task")
  public ApiResponse<CompetitionPlayerSortingTaskVO> saveRoles(
      @PathVariable Long taskId,
      @Valid @RequestBody CompetitionPlayerSortingRolesReq req) {
    return ApiResponse.ok(service.saveRoles(taskId, req));
  }

  @PutMapping("/tasks/{taskId}/workstations")
  @PreAuthorize("hasAuthority('competition:player-sorting:create')")
  @AuditLog(module = "competition", action = "player-sorting:workstations",
      targetType = "competition_practical_sorting_task")
  public ApiResponse<CompetitionPlayerSortingTaskVO> saveWorkstations(
      @PathVariable Long taskId,
      @Valid @RequestBody CompetitionPlayerSortingWorkstationsReq req) {
    return ApiResponse.ok(service.saveWorkstations(taskId, req));
  }

  @PostMapping("/tasks/{taskId}/draw-workstations")
  @PreAuthorize("hasAuthority('competition:player-sorting:randomize')")
  @AuditLog(module = "competition", action = "player-sorting:draw-workstations",
      targetType = "competition_practical_sorting_task")
  public ApiResponse<CompetitionPlayerSortingTaskVO> drawWorkstations(@PathVariable Long taskId) {
    return ApiResponse.ok(service.drawWorkstations(taskId));
  }

  @PostMapping("/tasks/{taskId}/randomize")
  @PreAuthorize("hasAuthority('competition:player-sorting:randomize')")
  @AuditLog(module = "competition", action = "player-sorting:randomize",
      targetType = "competition_practical_sorting_task")
  public ApiResponse<CompetitionPlayerSortingTaskVO> randomize(@PathVariable Long taskId) {
    return ApiResponse.ok(service.randomize(taskId));
  }

  @GetMapping("/tasks/{taskId}")
  @PreAuthorize("hasAuthority('competition:player-sorting:view')")
  public ApiResponse<CompetitionPlayerSortingTaskVO> get(@PathVariable Long taskId) {
    return ApiResponse.ok(service.get(taskId));
  }

  @GetMapping("/tasks/{taskId}/roster.xlsx")
  @PreAuthorize("hasAuthority('competition:player-sorting:export')")
  @AuditLog(module = "competition", action = "player-sorting:export",
      targetType = "competition_practical_sorting_task")
  public ResponseEntity<byte[]> exportExcel(@PathVariable Long taskId) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
            .filename("实操出场名单-任务" + taskId + ".xlsx", StandardCharsets.UTF_8)
            .build().toString())
        .body(service.exportExcel(taskId));
  }
}
