package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.service.TrainFollowTaskApplicationService;
import com.guoyu.platform.trainingdepartment.vo.FollowTaskItemVO;
import com.guoyu.platform.trainingdepartment.vo.FollowTaskResultVO;
import com.guoyu.platform.trainingdepartment.vo.FollowTaskStatsVO;
import com.guoyu.platform.trainingdepartment.vo.FollowTaskVO;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端跟班作业 —— 《跟班作业与随堂练实现方案》M3-a。
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
  public ApiResponse<List<FollowTaskVO>> list(@PathVariable Long classId) {
    return ApiResponse.ok(taskService.listByClassAdmin(classId));
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

  @PostMapping("/{id}/sync-roster")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-task:sync-roster", targetType = "train_follow_task")
  public ApiResponse<Map<String, Object>> syncRoster(@PathVariable Long id) {
    return ApiResponse.ok(taskService.syncRoster(id));
  }
}
