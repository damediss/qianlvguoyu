package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainSettlementCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSettlementQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSettlementUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainSettlementApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainSettlementVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 培训结算管理接口
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainSettlementController {

  private final TrainSettlementApplicationService trainSettlementApplicationService;

  public TrainSettlementController(TrainSettlementApplicationService trainSettlementApplicationService) {
    this.trainSettlementApplicationService = trainSettlementApplicationService;
  }

  /** 分页查询 */
  @GetMapping("/settlements")
  @PreAuthorize("hasAuthority('train:settlement:view')")
  public ApiResponse<PageResponse<TrainSettlementVO>> page(@Valid @ModelAttribute TrainSettlementQueryReq req) {
    return ApiResponse.ok(trainSettlementApplicationService.page(req));
  }

  /** 根据ID查询 */
  @GetMapping("/settlements/{id}")
  @PreAuthorize("hasAuthority('train:settlement:view')")
  public ApiResponse<TrainSettlementVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainSettlementApplicationService.getById(id));
  }

  /** 创建 */
  @PostMapping("/settlements")
  @PreAuthorize("hasAuthority('train:settlement:create')")
  @AuditLog(module = "training", action = "settlement:create", targetType = "train_settlement")
  public ApiResponse<TrainSettlementVO> create(@Valid @RequestBody TrainSettlementCreateReq req) {
    return ApiResponse.ok(trainSettlementApplicationService.create(req));
  }

  /** 更新 */
  @PutMapping("/settlements/{id}")
  @PreAuthorize("hasAuthority('train:settlement:update')")
  @AuditLog(module = "training", action = "settlement:update", targetType = "train_settlement")
  public ApiResponse<TrainSettlementVO> update(@PathVariable Long id, @Valid @RequestBody TrainSettlementUpdateReq req) {
    return ApiResponse.ok(trainSettlementApplicationService.update(id, req));
  }

  /** 删除 */
  @DeleteMapping("/settlements/{id}")
  @PreAuthorize("hasAuthority('train:settlement:delete')")
  @AuditLog(module = "training", action = "settlement:delete", targetType = "train_settlement")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainSettlementApplicationService.delete(id);
    return ApiResponse.ok();
  }

  /** 提交审批 */
  @PostMapping("/settlements/{id}/submit")
  @PreAuthorize("hasAuthority('train:settlement:update')")
  @AuditLog(module = "training", action = "settlement:submit", targetType = "train_settlement")
  public ApiResponse<TrainSettlementVO> submit(@PathVariable Long id) {
    return ApiResponse.ok(trainSettlementApplicationService.submit(id));
  }

  /** 审批通过 */
  @PostMapping("/settlements/{id}/approve")
  @PreAuthorize("hasAuthority('train:settlement:update')")
  @AuditLog(module = "training", action = "settlement:approve", targetType = "train_settlement")
  public ApiResponse<TrainSettlementVO> approve(@PathVariable Long id) {
    return ApiResponse.ok(trainSettlementApplicationService.approve(id));
  }

  /** 驳回 */
  @PostMapping("/settlements/{id}/reject")
  @PreAuthorize("hasAuthority('train:settlement:update')")
  @AuditLog(module = "training", action = "settlement:reject", targetType = "train_settlement")
  public ApiResponse<TrainSettlementVO> reject(@PathVariable Long id, @RequestParam String reason) {
    return ApiResponse.ok(trainSettlementApplicationService.reject(id, reason));
  }
}
