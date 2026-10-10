package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpOrcSelfCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpOrcSelfQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpOrcSelfUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainSignUpOrcSelfApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainSignUpOrcSelfVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training")
public class TrainSignUpOrcSelfController {

  private final TrainSignUpOrcSelfApplicationService trainSignUpOrcSelfApplicationService;

  public TrainSignUpOrcSelfController(TrainSignUpOrcSelfApplicationService trainSignUpOrcSelfApplicationService) {
    this.trainSignUpOrcSelfApplicationService = trainSignUpOrcSelfApplicationService;
  }

  @GetMapping("/sign-up-orcs")
  @PreAuthorize("hasAuthority('train:sign-up-orc:view')")
  public ApiResponse<PageResponse<TrainSignUpOrcSelfVO>> page(@Valid @ModelAttribute TrainSignUpOrcSelfQueryReq req) {
    return ApiResponse.ok(trainSignUpOrcSelfApplicationService.page(req));
  }

  @GetMapping("/sign-up-orcs/{id}")
  @PreAuthorize("hasAuthority('train:sign-up-orc:view')")
  public ApiResponse<TrainSignUpOrcSelfVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainSignUpOrcSelfApplicationService.getById(id));
  }

  @PostMapping("/sign-up-orcs")
  @PreAuthorize("hasAuthority('train:sign-up-orc:create')")
  @AuditLog(module = "training", action = "sign-up-orc:create", targetType = "sign-up-orcs")
  public ApiResponse<TrainSignUpOrcSelfVO> create(@Valid @RequestBody TrainSignUpOrcSelfCreateReq req) {
    return ApiResponse.ok(trainSignUpOrcSelfApplicationService.create(req));
  }

  @PutMapping("/sign-up-orcs/{id}")
  @PreAuthorize("hasAuthority('train:sign-up-orc:update')")
  @AuditLog(module = "training", action = "sign-up-orc:update", targetType = "sign-up-orcs")
  public ApiResponse<TrainSignUpOrcSelfVO> update(@PathVariable Long id, @Valid @RequestBody TrainSignUpOrcSelfUpdateReq req) {
    return ApiResponse.ok(trainSignUpOrcSelfApplicationService.update(id, req));
  }

  @DeleteMapping("/sign-up-orcs/{id}")
  @PreAuthorize("hasAuthority('train:sign-up-orc:delete')")
  @AuditLog(module = "training", action = "sign-up-orc:delete", targetType = "sign-up-orcs")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainSignUpOrcSelfApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
