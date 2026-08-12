package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.FollowBankDrawReq;
import com.guoyu.platform.trainingdepartment.dto.FollowTaskSaveReq;
import com.guoyu.platform.trainingdepartment.service.TrainFollowTaskApplicationService;
import com.guoyu.platform.trainingdepartment.vo.FollowTaskItemVO;
import com.guoyu.platform.trainingdepartment.vo.FollowTaskResultVO;
import com.guoyu.platform.trainingdepartment.vo.FollowTaskStatsVO;
import com.guoyu.platform.trainingdepartment.vo.FollowTaskVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
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

/**
 * 管理端跟班作业 / 模拟考 —— 《跟班作业与随堂练》+《跟班班级模拟试卷》。
 */
@Validated
@RestController
@RequestMapping("/training/follow-tasks")
public class TrainFollowTaskAdminController {

  private final TrainFollowTaskApplicationService taskService;

  public TrainFollowTaskAdminController(TrainFollowTaskApplicationService taskService) {
    this.taskService = taskService;
  }

  @GetMapping("/classes/{classId}/tasks")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<List<FollowTaskVO>> list(
      @PathVariable Long classId,
      @RequestParam(required = false, defaultValue = "assignment") String taskType) {
    return ApiResponse.ok(taskService.listByClassAdmin(classId, taskType));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-task:create", targetType = "train_follow_task")
  public ApiResponse<FollowTaskVO> create(@Valid @RequestBody FollowTaskSaveReq body) {
    return ApiResponse.ok(taskService.createDraftAdmin(body));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-task:update", targetType = "train_follow_task")
  public ApiResponse<FollowTaskVO> update(
      @PathVariable Long id, @Valid @RequestBody FollowTaskSaveReq body) {
    return ApiResponse.ok(taskService.updateDraftAdmin(id, body));
  }

  @PostMapping("/{id}/publish")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-task:publish", targetType = "train_follow_task")
  public ApiResponse<FollowTaskVO> publish(@PathVariable Long id) {
    return ApiResponse.ok(taskService.publishAdmin(id));
  }

  @PostMapping("/{id}/close")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-task:close", targetType = "train_follow_task")
  public ApiResponse<FollowTaskVO> close(@PathVariable Long id) {
    return ApiResponse.ok(taskService.closeAdmin(id));
  }

  @PostMapping("/{id}/reopen")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-task:reopen", targetType = "train_follow_task")
  public ApiResponse<FollowTaskVO> reopen(@PathVariable Long id) {
    return ApiResponse.ok(taskService.reopenAdmin(id));
  }

  @GetMapping("/classes/{classId}/mock-used-question-ids")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<Map<String, Object>> mockUsedQuestionIds(
      @PathVariable Long classId,
      @RequestParam(required = false) Long excludeTaskId) {
    return ApiResponse.ok(taskService.listMockUsedQuestionIds(classId, excludeTaskId));
  }

  @GetMapping("/classes/{classId}/bank-scope")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<Map<String, Object>> bankScope(@PathVariable Long classId) {
    return ApiResponse.ok(taskService.bankScopeMetaAdmin(classId));
  }

  @GetMapping("/classes/{classId}/bank-type-counts")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<Map<String, Object>> bankTypeCounts(
      @PathVariable Long classId,
      @RequestParam Integer courseId,
      @RequestParam(required = false, defaultValue = "false") boolean excludeOtherMock,
      @RequestParam(required = false) Long excludeTaskId) {
    return ApiResponse.ok(
        taskService.bankTypeCountsAdmin(classId, courseId, excludeOtherMock, excludeTaskId));
  }

  @PostMapping("/classes/{classId}/bank-draw")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  public ApiResponse<Map<String, Object>> bankDraw(
      @PathVariable Long classId, @Valid @RequestBody FollowBankDrawReq body) {
    return ApiResponse.ok(taskService.drawBankQuestionsAdmin(classId, body));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<FollowTaskVO> detail(@PathVariable Long id) {
    return ApiResponse.ok(taskService.getTaskAdmin(id));
  }

  @GetMapping("/{id}/items")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<List<FollowTaskItemVO>> items(@PathVariable Long id) {
    return ApiResponse.ok(taskService.listItemsAdmin(id));
  }

  @GetMapping("/{id}/items/{itemId}/answers")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<FollowTaskResultVO> answers(
      @PathVariable Long id, @PathVariable Long itemId) {
    return ApiResponse.ok(taskService.getItemAnswersAdmin(id, itemId));
  }

  @GetMapping("/{id}/stats")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<FollowTaskStatsVO> stats(@PathVariable Long id) {
    return ApiResponse.ok(taskService.getTaskStatsAdmin(id));
  }

  @GetMapping("/{id}/export-answers")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ResponseEntity<byte[]> exportAnswers(@PathVariable Long id) {
    return taskService.exportTaskAnswersExcel(id);
  }

  @GetMapping("/classes/{classId}/homework-metrics")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<Map<String, Object>> metrics(@PathVariable Long classId) {
    return ApiResponse.ok(taskService.homeworkMetrics(classId));
  }

  @GetMapping("/classes/{classId}/mock-metrics")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<Map<String, Object>> mockMetrics(@PathVariable Long classId) {
    return ApiResponse.ok(taskService.mockMetrics(classId));
  }

  @PostMapping("/{id}/sync-roster")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-task:sync-roster", targetType = "train_follow_task")
  public ApiResponse<Map<String, Object>> syncRoster(@PathVariable Long id) {
    return ApiResponse.ok(taskService.syncRoster(id));
  }
}
