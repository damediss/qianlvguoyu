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

@Validated
@RestController
@RequestMapping("/training")
public class TrainRequestController {

  private final TrainRequestService service;

  public TrainRequestController(TrainRequestService service) {
    this.service = service;
  }

  @GetMapping("/requests")
  @PreAuthorize("hasAuthority('train:request:view')")
  public ApiResponse<PageResponse<TrainRequestVO>> page(@Valid @ModelAttribute TrainRequestQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/requests/{id}")
  @PreAuthorize("hasAuthority('train:request:view')")
  public ApiResponse<TrainRequestVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  @PostMapping("/requests")
  @PreAuthorize("hasAuthority('train:request:create')")
  @AuditLog(module = "training", action = "request:create", targetType = "train_request")
  public ApiResponse<TrainRequestVO> create(@Valid @RequestBody TrainRequestCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  @PutMapping("/requests/{id}")
  @PreAuthorize("hasAuthority('train:request:update')")
  @AuditLog(module = "training", action = "request:update", targetType = "train_request")
  public ApiResponse<TrainRequestVO> update(@PathVariable Long id, @Valid @RequestBody TrainRequestUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  @DeleteMapping("/requests/{id}")
  @PreAuthorize("hasAuthority('train:request:delete')")
  @AuditLog(module = "training", action = "request:delete", targetType = "train_request")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
