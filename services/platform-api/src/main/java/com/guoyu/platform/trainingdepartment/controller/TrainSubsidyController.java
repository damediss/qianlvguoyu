package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainSubsidyCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSubsidyQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSubsidyUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainSubsidyApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainSubsidyVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 培训补贴管理接口
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainSubsidyController {

  private final TrainSubsidyApplicationService trainSubsidyApplicationService;

  public TrainSubsidyController(TrainSubsidyApplicationService trainSubsidyApplicationService) {
    this.trainSubsidyApplicationService = trainSubsidyApplicationService;
  }

  /** 分页查询 */
  @GetMapping("/subsidies")
  @PreAuthorize("hasAuthority('train:subsidy:view')")
  public ApiResponse<PageResponse<TrainSubsidyVO>> page(@Valid @ModelAttribute TrainSubsidyQueryReq req) {
    return ApiResponse.ok(trainSubsidyApplicationService.page(req));
  }

  /** 根据ID查询 */
  @GetMapping("/subsidies/{id}")
  @PreAuthorize("hasAuthority('train:subsidy:view')")
  public ApiResponse<TrainSubsidyVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainSubsidyApplicationService.getById(id));
  }

  /** 创建 */
  @PostMapping("/subsidies")
  @PreAuthorize("hasAuthority('train:subsidy:create')")
  @AuditLog(module = "training", action = "subsidy:create", targetType = "train_subsidy")
  public ApiResponse<TrainSubsidyVO> create(@Valid @RequestBody TrainSubsidyCreateReq req) {
    return ApiResponse.ok(trainSubsidyApplicationService.create(req));
  }

  /** 更新 */
  @PutMapping("/subsidies/{id}")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:update", targetType = "train_subsidy")
  public ApiResponse<TrainSubsidyVO> update(@PathVariable Long id, @Valid @RequestBody TrainSubsidyUpdateReq req) {
    return ApiResponse.ok(trainSubsidyApplicationService.update(id, req));
  }

  /** 删除 */
  @DeleteMapping("/subsidies/{id}")
  @PreAuthorize("hasAuthority('train:subsidy:delete')")
  @AuditLog(module = "training", action = "subsidy:delete", targetType = "train_subsidy")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainSubsidyApplicationService.delete(id);
    return ApiResponse.ok();
  }

  /** 提交审批 */
  @PostMapping("/subsidies/{id}/submit")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:submit", targetType = "train_subsidy")
  public ApiResponse<TrainSubsidyVO> submit(@PathVariable Long id) {
    return ApiResponse.ok(trainSubsidyApplicationService.submit(id));
  }

  /** 审批通过 */
  @PostMapping("/subsidies/{id}/approve")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:approve", targetType = "train_subsidy")
  public ApiResponse<TrainSubsidyVO> approve(@PathVariable Long id) {
    return ApiResponse.ok(trainSubsidyApplicationService.approve(id));
  }

  /** 驳回 */
  @PostMapping("/subsidies/{id}/reject")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:reject", targetType = "train_subsidy")
  public ApiResponse<TrainSubsidyVO> reject(@PathVariable Long id, @RequestParam String reason) {
    return ApiResponse.ok(trainSubsidyApplicationService.reject(id, reason));
  }
}
