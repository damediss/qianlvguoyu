package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskInfoCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskInfoQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskInfoUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainTaskInfoApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainTaskInfoVO;
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
 * 培训配置任务列表管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainTaskInfoController {

  private final TrainTaskInfoApplicationService trainTaskInfoApplicationService;

  public TrainTaskInfoController(TrainTaskInfoApplicationService trainTaskInfoApplicationService) {
    this.trainTaskInfoApplicationService = trainTaskInfoApplicationService;
  }

  /** 分页查询培训配置任务列表 */
  @GetMapping("/task-infos")
  @PreAuthorize("hasAuthority('train:task-info:view')")
  public ApiResponse<PageResponse<TrainTaskInfoVO>> page(@Valid @ModelAttribute TrainTaskInfoQueryReq req) {
    return ApiResponse.ok(trainTaskInfoApplicationService.page(req));
  }

  /** 根据ID查询培训配置任务详情 */
  @GetMapping("/task-infos/{id}")
  @PreAuthorize("hasAuthority('train:task-info:view')")
  public ApiResponse<TrainTaskInfoVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainTaskInfoApplicationService.getById(id));
  }

  /** 新增培训配置任务 */
  @PostMapping("/task-infos")
  @PreAuthorize("hasAuthority('train:task-info:create')")
  @AuditLog(module = "training", action = "task-info:create", targetType = "train_task_info")
  public ApiResponse<TrainTaskInfoVO> create(@Valid @RequestBody TrainTaskInfoCreateReq req) {
    return ApiResponse.ok(trainTaskInfoApplicationService.create(req));
  }

  /** 修改培训配置任务 */
  @PutMapping("/task-infos/{id}")
  @PreAuthorize("hasAuthority('train:task-info:update')")
  @AuditLog(module = "training", action = "task-info:update", targetType = "train_task_info")
  public ApiResponse<TrainTaskInfoVO> update(@PathVariable Long id, @Valid @RequestBody TrainTaskInfoUpdateReq req) {
    return ApiResponse.ok(trainTaskInfoApplicationService.update(id, req));
  }

  /** 删除培训配置任务 */
  @DeleteMapping("/task-infos/{id}")
  @PreAuthorize("hasAuthority('train:task-info:delete')")
  @AuditLog(module = "training", action = "task-info:delete", targetType = "train_task_info")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainTaskInfoApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
