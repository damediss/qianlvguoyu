package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainBaseDataCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainBaseDataQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainBaseDataUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainBaseDataApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainBaseDataVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training")
public class TrainBaseDataController {

  private final TrainBaseDataApplicationService trainBaseDataApplicationService;

  public TrainBaseDataController(TrainBaseDataApplicationService trainBaseDataApplicationService) {
    this.trainBaseDataApplicationService = trainBaseDataApplicationService;
  }

  @GetMapping("/base-data")
  @PreAuthorize("hasAuthority('train:base-data:view')")
  public ApiResponse<PageResponse<TrainBaseDataVO>> page(@Valid @ModelAttribute TrainBaseDataQueryReq req) {
    return ApiResponse.ok(trainBaseDataApplicationService.page(req));
  }

  @GetMapping("/base-data/{id}")
  @PreAuthorize("hasAuthority('train:base-data:view')")
  public ApiResponse<TrainBaseDataVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainBaseDataApplicationService.getById(id));
  }

  @PostMapping("/base-data")
  @PreAuthorize("hasAuthority('train:base-data:create')")
  @AuditLog(module = "training", action = "base-data:create", targetType = "base-data")
  public ApiResponse<TrainBaseDataVO> create(@Valid @RequestBody TrainBaseDataCreateReq req) {
    return ApiResponse.ok(trainBaseDataApplicationService.create(req));
  }

  @PutMapping("/base-data/{id}")
  @PreAuthorize("hasAuthority('train:base-data:update')")
  @AuditLog(module = "training", action = "base-data:update", targetType = "base-data")
  public ApiResponse<TrainBaseDataVO> update(@PathVariable Long id, @Valid @RequestBody TrainBaseDataUpdateReq req) {
    return ApiResponse.ok(trainBaseDataApplicationService.update(id, req));
  }

  @DeleteMapping("/base-data/{id}")
  @PreAuthorize("hasAuthority('train:base-data:delete')")
  @AuditLog(module = "training", action = "base-data:delete", targetType = "base-data")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainBaseDataApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
