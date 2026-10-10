package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpSelfCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpSelfQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpSelfUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainSignUpSelfApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainSignUpSelfVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training")
public class TrainSignUpSelfController {

  private final TrainSignUpSelfApplicationService trainSignUpSelfApplicationService;

  public TrainSignUpSelfController(TrainSignUpSelfApplicationService trainSignUpSelfApplicationService) {
    this.trainSignUpSelfApplicationService = trainSignUpSelfApplicationService;
  }

  @GetMapping("/sign-ups")
  @PreAuthorize("hasAuthority('train:sign-up:view')")
  public ApiResponse<PageResponse<TrainSignUpSelfVO>> page(@Valid @ModelAttribute TrainSignUpSelfQueryReq req) {
    return ApiResponse.ok(trainSignUpSelfApplicationService.page(req));
  }

  @GetMapping("/sign-ups/{id}")
  @PreAuthorize("hasAuthority('train:sign-up:view')")
  public ApiResponse<TrainSignUpSelfVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainSignUpSelfApplicationService.getById(id));
  }

  @PostMapping("/sign-ups")
  @PreAuthorize("hasAuthority('train:sign-up:create')")
  @AuditLog(module = "training", action = "sign-up:create", targetType = "sign-ups")
  public ApiResponse<TrainSignUpSelfVO> create(@Valid @RequestBody TrainSignUpSelfCreateReq req) {
    return ApiResponse.ok(trainSignUpSelfApplicationService.create(req));
  }

  @PutMapping("/sign-ups/{id}")
  @PreAuthorize("hasAuthority('train:sign-up:update')")
  @AuditLog(module = "training", action = "sign-up:update", targetType = "sign-ups")
  public ApiResponse<TrainSignUpSelfVO> update(@PathVariable Long id, @Valid @RequestBody TrainSignUpSelfUpdateReq req) {
    return ApiResponse.ok(trainSignUpSelfApplicationService.update(id, req));
  }

  @DeleteMapping("/sign-ups/{id}")
  @PreAuthorize("hasAuthority('train:sign-up:delete')")
  @AuditLog(module = "training", action = "sign-up:delete", targetType = "sign-ups")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainSignUpSelfApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
