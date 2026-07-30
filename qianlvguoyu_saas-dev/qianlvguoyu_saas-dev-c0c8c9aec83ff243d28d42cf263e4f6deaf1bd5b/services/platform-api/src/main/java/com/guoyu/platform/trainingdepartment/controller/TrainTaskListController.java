package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskListUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainTaskListApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainTaskListVO;
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
 * 知识点任务列表管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainTaskListController {

  private final TrainTaskListApplicationService trainTaskListApplicationService;

  public TrainTaskListController(TrainTaskListApplicationService trainTaskListApplicationService) {
    this.trainTaskListApplicationService = trainTaskListApplicationService;
  }

  /** 分页查询知识点任务列表 */
  @GetMapping("/task-lists")
  @PreAuthorize("hasAuthority('train:task-list:view')")
  public ApiResponse<PageResponse<TrainTaskListVO>> page(@Valid @ModelAttribute TrainTaskListQueryReq req) {
    return ApiResponse.ok(trainTaskListApplicationService.page(req));
  }

  /** 根据ID查询知识点任务详情 */
  @GetMapping("/task-lists/{id}")
  @PreAuthorize("hasAuthority('train:task-list:view')")
  public ApiResponse<TrainTaskListVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainTaskListApplicationService.getById(id));
  }

  /** 新增知识点任务 */
  @PostMapping("/task-lists")
  @PreAuthorize("hasAuthority('train:task-list:create')")
  @AuditLog(module = "training", action = "task-list:create", targetType = "train_task_list")
  public ApiResponse<TrainTaskListVO> create(@Valid @RequestBody TrainTaskListCreateReq req) {
    return ApiResponse.ok(trainTaskListApplicationService.create(req));
  }

  /** 修改知识点任务 */
  @PutMapping("/task-lists/{id}")
  @PreAuthorize("hasAuthority('train:task-list:update')")
  @AuditLog(module = "training", action = "task-list:update", targetType = "train_task_list")
  public ApiResponse<TrainTaskListVO> update(@PathVariable Long id, @Valid @RequestBody TrainTaskListUpdateReq req) {
    return ApiResponse.ok(trainTaskListApplicationService.update(id, req));
  }

  /** 删除知识点任务 */
  @DeleteMapping("/task-lists/{id}")
  @PreAuthorize("hasAuthority('train:task-list:delete')")
  @AuditLog(module = "training", action = "task-list:delete", targetType = "train_task_list")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainTaskListApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
