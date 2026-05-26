package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainProjectCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainProjectQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainProjectUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainProjectApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainProjectTableVO;
import com.guoyu.platform.trainingdepartment.vo.TrainProjectVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 培训项目管理接口
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainProjectController {

  private final TrainProjectApplicationService trainProjectApplicationService;

  public TrainProjectController(TrainProjectApplicationService trainProjectApplicationService) {
    this.trainProjectApplicationService = trainProjectApplicationService;
  }

  /** 分页查询 */
  @GetMapping("/projects")
  @PreAuthorize("hasAuthority('train:project:view')")
  public ApiResponse<PageResponse<TrainProjectVO>> page(@Valid @ModelAttribute TrainProjectQueryReq req) {
    return ApiResponse.ok(trainProjectApplicationService.page(req));
  }

  /** 根据ID查询 */
  @GetMapping("/projects/{id}")
  @PreAuthorize("hasAuthority('train:project:view')")
  public ApiResponse<TrainProjectVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainProjectApplicationService.getById(id));
  }

  /** 创建 */
  @PostMapping("/projects")
  @PreAuthorize("hasAuthority('train:project:create')")
  @AuditLog(module = "training", action = "project:create", targetType = "train_project")
  public ApiResponse<TrainProjectVO> create(@Valid @RequestBody TrainProjectCreateReq req) {
    return ApiResponse.ok(trainProjectApplicationService.create(req));
  }

  /** 更新 */
  @PutMapping("/projects/{id}")
  @PreAuthorize("hasAuthority('train:project:update')")
  @AuditLog(module = "training", action = "project:update", targetType = "train_project")
  public ApiResponse<TrainProjectVO> update(@PathVariable Long id, @Valid @RequestBody TrainProjectUpdateReq req) {
    return ApiResponse.ok(trainProjectApplicationService.update(id, req));
  }

  /** 删除 */
  @DeleteMapping("/projects/{id}")
  @PreAuthorize("hasAuthority('train:project:delete')")
  @AuditLog(module = "training", action = "project:delete", targetType = "train_project")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainProjectApplicationService.delete(id);
    return ApiResponse.ok();
  }

  /** 发起申请 */
  @PostMapping("/projects/{id}/launch")
  @PreAuthorize("hasAuthority('train:project:update')")
  @AuditLog(module = "training", action = "project:launch", targetType = "train_project")
  public ApiResponse<TrainProjectVO> launch(@PathVariable Long id) {
    return ApiResponse.ok(trainProjectApplicationService.launch(id));
  }

  /** 审批通过 */
  @PostMapping("/projects/{id}/approve")
  @PreAuthorize("hasAuthority('train:project:update')")
  @AuditLog(module = "training", action = "project:approve", targetType = "train_project")
  public ApiResponse<TrainProjectVO> approve(@PathVariable Long id) {
    return ApiResponse.ok(trainProjectApplicationService.approve(id));
  }

  /** 驳回 */
  @PostMapping("/projects/{id}/reject")
  @PreAuthorize("hasAuthority('train:project:update')")
  @AuditLog(module = "training", action = "project:reject", targetType = "train_project")
  public ApiResponse<TrainProjectVO> reject(@PathVariable Long id, @RequestParam String reason) {
    return ApiResponse.ok(trainProjectApplicationService.reject(id, reason));
  }

  /** 流转阶段 */
  @PostMapping("/projects/{id}/transit")
  @PreAuthorize("hasAuthority('train:project:update')")
  @AuditLog(module = "training", action = "project:transit", targetType = "train_project")
  public ApiResponse<TrainProjectVO> transit(@PathVariable Long id, @RequestParam String targetStage) {
    return ApiResponse.ok(trainProjectApplicationService.transit(id, targetStage));
  }

  @GetMapping("/projects/{id}/tables")
  @PreAuthorize("hasAuthority('train:project:view')")
  public ApiResponse<List<TrainProjectTableVO>> getProjectTables(@PathVariable Long id) {
    return ApiResponse.ok(trainProjectApplicationService.getProjectTables(id));
  }
}
