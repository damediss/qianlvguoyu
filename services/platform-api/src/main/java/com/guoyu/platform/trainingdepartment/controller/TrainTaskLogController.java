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

/**
 * 培训任务记录管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainTaskLogController {

  private final TrainTaskLogApplicationService trainTaskLogApplicationService;

  public TrainTaskLogController(TrainTaskLogApplicationService trainTaskLogApplicationService) {
    this.trainTaskLogApplicationService = trainTaskLogApplicationService;
  }

  /** 分页查询培训任务记录列表 */
  @GetMapping("/task-logs")
  @PreAuthorize("hasAuthority('train:task-log:view')")
  public ApiResponse<PageResponse<TrainTaskLogVO>> page(@Valid @ModelAttribute TrainTaskLogQueryReq req) {
    return ApiResponse.ok(trainTaskLogApplicationService.page(req));
  }

  /** 根据ID查询培训任务记录详情 */
  @GetMapping("/task-logs/{id}")
  @PreAuthorize("hasAuthority('train:task-log:view')")
  public ApiResponse<TrainTaskLogVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainTaskLogApplicationService.getById(id));
  }

  /** 新增培训任务记录 */
  @PostMapping("/task-logs")
  @PreAuthorize("hasAuthority('train:task-log:create')")
  @AuditLog(module = "training", action = "task-log:create", targetType = "train_task_log")
  public ApiResponse<TrainTaskLogVO> create(@Valid @RequestBody TrainTaskLogCreateReq req) {
    return ApiResponse.ok(trainTaskLogApplicationService.create(req));
  }

  /** 修改培训任务记录 */
  @PutMapping("/task-logs/{id}")
  @PreAuthorize("hasAuthority('train:task-log:update')")
  @AuditLog(module = "training", action = "task-log:update", targetType = "train_task_log")
  public ApiResponse<TrainTaskLogVO> update(@PathVariable Long id, @Valid @RequestBody TrainTaskLogUpdateReq req) {
    return ApiResponse.ok(trainTaskLogApplicationService.update(id, req));
  }

  /** 删除培训任务记录 */
  @DeleteMapping("/task-logs/{id}")
  @PreAuthorize("hasAuthority('train:task-log:delete')")
  @AuditLog(module = "training", action = "task-log:delete", targetType = "train_task_log")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainTaskLogApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
