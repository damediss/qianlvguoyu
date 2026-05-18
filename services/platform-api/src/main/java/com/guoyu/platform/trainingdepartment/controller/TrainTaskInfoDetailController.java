package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskInfoDetailCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskInfoDetailQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskInfoDetailUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainTaskInfoDetailService;
import com.guoyu.platform.trainingdepartment.vo.TrainTaskInfoDetailVO;
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
 * 任务详情管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainTaskInfoDetailController {

  private final TrainTaskInfoDetailService service;

  public TrainTaskInfoDetailController(TrainTaskInfoDetailService service) {
    this.service = service;
  }

  /** 分页查询任务详情列表 */
  @GetMapping("/task-info-details")
  @PreAuthorize("hasAuthority('train:task-info-detail:view')")
  public ApiResponse<PageResponse<TrainTaskInfoDetailVO>> page(@Valid @ModelAttribute TrainTaskInfoDetailQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  /** 根据ID查询任务详情 */
  @GetMapping("/task-info-details/{id}")
  @PreAuthorize("hasAuthority('train:task-info-detail:view')")
  public ApiResponse<TrainTaskInfoDetailVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  /** 新增任务详情 */
  @PostMapping("/task-info-details")
  @PreAuthorize("hasAuthority('train:task-info-detail:create')")
  @AuditLog(module = "training", action = "task-info-detail:create", targetType = "train_task_info_detail")
  public ApiResponse<TrainTaskInfoDetailVO> create(@Valid @RequestBody TrainTaskInfoDetailCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  /** 修改任务详情 */
  @PutMapping("/task-info-details/{id}")
  @PreAuthorize("hasAuthority('train:task-info-detail:update')")
  @AuditLog(module = "training", action = "task-info-detail:update", targetType = "train_task_info_detail")
  public ApiResponse<TrainTaskInfoDetailVO> update(@PathVariable Long id, @Valid @RequestBody TrainTaskInfoDetailUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  /** 删除任务详情 */
  @DeleteMapping("/task-info-details/{id}")
  @PreAuthorize("hasAuthority('train:task-info-detail:delete')")
  @AuditLog(module = "training", action = "task-info-detail:delete", targetType = "train_task_info_detail")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
