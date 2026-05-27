package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskLogCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskLogQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskLogUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainTaskLogApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainTaskLogVO;
import jakarta.validation.Valid;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/training")
public class TrainTaskLogController {

  private final TrainTaskLogApplicationService trainTaskLogApplicationService;

  public TrainTaskLogController(TrainTaskLogApplicationService trainTaskLogApplicationService) {
    this.trainTaskLogApplicationService = trainTaskLogApplicationService;
  }

  // ==================== 基础CRUD ====================

  @GetMapping("/task-logs")
  @PreAuthorize("hasAuthority('train:task-log:view')")
  public ApiResponse<PageResponse<TrainTaskLogVO>> page(@Valid @ModelAttribute TrainTaskLogQueryReq req) {
    return ApiResponse.ok(trainTaskLogApplicationService.page(req));
  }

  @GetMapping("/task-logs/{id}")
  @PreAuthorize("hasAuthority('train:task-log:view')")
  public ApiResponse<TrainTaskLogVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainTaskLogApplicationService.getById(id));
  }

  @PostMapping("/task-logs")
  @PreAuthorize("hasAuthority('train:task-log:create')")
  @AuditLog(module = "training", action = "task-log:create", targetType = "train_task_log")
  public ApiResponse<TrainTaskLogVO> create(@Valid @RequestBody TrainTaskLogCreateReq req) {
    return ApiResponse.ok(trainTaskLogApplicationService.create(req));
  }

  @PutMapping("/task-logs/{id}")
  @PreAuthorize("hasAuthority('train:task-log:update')")
  @AuditLog(module = "training", action = "task-log:update", targetType = "train_task_log")
  public ApiResponse<TrainTaskLogVO> update(@PathVariable Long id, @Valid @RequestBody TrainTaskLogUpdateReq req) {
    return ApiResponse.ok(trainTaskLogApplicationService.update(id, req));
  }

  @DeleteMapping("/task-logs/{id}")
  @PreAuthorize("hasAuthority('train:task-log:delete')")
  @AuditLog(module = "training", action = "task-log:delete", targetType = "train_task_log")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainTaskLogApplicationService.delete(id);
    return ApiResponse.ok();
  }

  // ==================== 班级培训任务 ====================

  /** 班级培训任务列表（含课件名称映射） */
  @GetMapping("/class-task")
  @PreAuthorize("hasAuthority('train:task-log:view')")
  public ApiResponse<Map<String, Object>> classTaskList(@RequestParam Long classId) {
    return ApiResponse.ok(trainTaskLogApplicationService.classTaskList(classId));
  }

  /** 新增/编辑培训任务 */
  @PostMapping("/class-task-save")
  @PreAuthorize("hasAuthority('train:task-log:create')")
  @AuditLog(module = "training", action = "class-task:save", targetType = "train_task_log")
  public ApiResponse<Map<String, Object>> saveTask(@RequestBody Map<String, Object> params) {
    return ApiResponse.ok(trainTaskLogApplicationService.saveTask(params));
  }
}
