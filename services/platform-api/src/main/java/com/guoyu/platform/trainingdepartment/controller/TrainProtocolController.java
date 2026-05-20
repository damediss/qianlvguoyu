package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainProtocolCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainProtocolQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainProtocolUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainProtocolService;
import com.guoyu.platform.trainingdepartment.vo.TrainProtocolVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 培训协议管理接口
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainProtocolController {

  private final TrainProtocolService service;

  public TrainProtocolController(TrainProtocolService service) {
    this.service = service;
  }

  /** 分页查询 */
  @GetMapping("/protocols")
  @PreAuthorize("hasAuthority('train:protocol:view')")
  public ApiResponse<PageResponse<TrainProtocolVO>> page(@Valid @ModelAttribute TrainProtocolQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  /** 根据ID查询 */
  @GetMapping("/protocols/{id}")
  @PreAuthorize("hasAuthority('train:protocol:view')")
  public ApiResponse<TrainProtocolVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  /** 创建 */
  @PostMapping("/protocols")
  @PreAuthorize("hasAuthority('train:protocol:create')")
  @AuditLog(module = "training", action = "protocol:create", targetType = "train_protocol")
  public ApiResponse<TrainProtocolVO> create(@Valid @RequestBody TrainProtocolCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  /** 更新 */
  @PutMapping("/protocols/{id}")
  @PreAuthorize("hasAuthority('train:protocol:update')")
  @AuditLog(module = "training", action = "protocol:update", targetType = "train_protocol")
  public ApiResponse<TrainProtocolVO> update(@PathVariable Long id, @Valid @RequestBody TrainProtocolUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  /** 删除 */
  @DeleteMapping("/protocols/{id}")
  @PreAuthorize("hasAuthority('train:protocol:delete')")
  @AuditLog(module = "training", action = "protocol:delete", targetType = "train_protocol")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }

  /** 提交审核 */
  @PostMapping("/protocols/{id}/submit-review")
  @PreAuthorize("hasAuthority('train:protocol:update')")
  @AuditLog(module = "training", action = "protocol:submit-review", targetType = "train_protocol")
  public ApiResponse<TrainProtocolVO> submitReview(@PathVariable Long id) {
    return ApiResponse.ok(service.submitReview(id));
  }

  /** 办公室审批 */
  @PostMapping("/protocols/{id}/office-approve")
  @PreAuthorize("hasAuthority('train:protocol:update')")
  @AuditLog(module = "training", action = "protocol:office-approve", targetType = "train_protocol")
  public ApiResponse<TrainProtocolVO> officeApprove(@PathVariable Long id) {
    return ApiResponse.ok(service.officeApprove(id));
  }

  /** 总经理审批 */
  @PostMapping("/protocols/{id}/gm-approve")
  @PreAuthorize("hasAuthority('train:protocol:update')")
  @AuditLog(module = "training", action = "protocol:gm-approve", targetType = "train_protocol")
  public ApiResponse<TrainProtocolVO> gmApprove(@PathVariable Long id) {
    return ApiResponse.ok(service.gmApprove(id));
  }

  /** 企业确认 */
  @PostMapping("/protocols/{id}/enterprise-confirm")
  @PreAuthorize("hasAuthority('train:protocol:update')")
  @AuditLog(module = "training", action = "protocol:enterprise-confirm", targetType = "train_protocol")
  public ApiResponse<TrainProtocolVO> enterpriseConfirm(@PathVariable Long id) {
    return ApiResponse.ok(service.enterpriseConfirm(id));
  }

  /** 签署 */
  @PostMapping("/protocols/{id}/sign")
  @PreAuthorize("hasAuthority('train:protocol:update')")
  @AuditLog(module = "training", action = "protocol:sign", targetType = "train_protocol")
  public ApiResponse<TrainProtocolVO> sign(@PathVariable Long id) {
    return ApiResponse.ok(service.sign(id));
  }

  /** 驳回 */
  @PostMapping("/protocols/{id}/reject")
  @PreAuthorize("hasAuthority('train:protocol:update')")
  @AuditLog(module = "training", action = "protocol:reject", targetType = "train_protocol")
  public ApiResponse<TrainProtocolVO> reject(@PathVariable Long id, @RequestParam String reason) {
    return ApiResponse.ok(service.reject(id, reason));
  }
}
