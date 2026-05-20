package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainRequestCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainRequestQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainRequestUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainRequestService;
import com.guoyu.platform.trainingdepartment.vo.TrainRequestVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 培训申请管理接口
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainRequestController {

  private final TrainRequestService service;

  public TrainRequestController(TrainRequestService service) {
    this.service = service;
  }

  /** 分页查询 */
  @GetMapping("/requests")
  @PreAuthorize("hasAuthority('train:request:view')")
  public ApiResponse<PageResponse<TrainRequestVO>> page(@Valid @ModelAttribute TrainRequestQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  /** 根据ID查询 */
  @GetMapping("/requests/{id}")
  @PreAuthorize("hasAuthority('train:request:view')")
  public ApiResponse<TrainRequestVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  /** 创建 */
  @PostMapping("/requests")
  @PreAuthorize("hasAuthority('train:request:create')")
  @AuditLog(module = "training", action = "request:create", targetType = "train_request")
  public ApiResponse<TrainRequestVO> create(@Valid @RequestBody TrainRequestCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  /** 更新 */
  @PutMapping("/requests/{id}")
  @PreAuthorize("hasAuthority('train:request:update')")
  @AuditLog(module = "training", action = "request:update", targetType = "train_request")
  public ApiResponse<TrainRequestVO> update(@PathVariable Long id, @Valid @RequestBody TrainRequestUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  /** 删除 */
  @DeleteMapping("/requests/{id}")
  @PreAuthorize("hasAuthority('train:request:delete')")
  @AuditLog(module = "training", action = "request:delete", targetType = "train_request")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
