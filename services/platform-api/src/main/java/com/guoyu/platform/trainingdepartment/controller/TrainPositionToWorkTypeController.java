package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainPositionToWorkTypeCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPositionToWorkTypeQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPositionToWorkTypeUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainPositionToWorkTypeApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainPositionToWorkTypeVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training")
public class TrainPositionToWorkTypeController {

  private final TrainPositionToWorkTypeApplicationService trainPositionToWorkTypeApplicationService;

  public TrainPositionToWorkTypeController(TrainPositionToWorkTypeApplicationService trainPositionToWorkTypeApplicationService) {
    this.trainPositionToWorkTypeApplicationService = trainPositionToWorkTypeApplicationService;
  }

  @GetMapping("/position-worktypes")
  @PreAuthorize("hasAuthority('train:position-worktype:view')")
  public ApiResponse<PageResponse<TrainPositionToWorkTypeVO>> page(@Valid @ModelAttribute TrainPositionToWorkTypeQueryReq req) {
    return ApiResponse.ok(trainPositionToWorkTypeApplicationService.page(req));
  }

  @GetMapping("/position-worktypes/{id}")
  @PreAuthorize("hasAuthority('train:position-worktype:view')")
  public ApiResponse<TrainPositionToWorkTypeVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainPositionToWorkTypeApplicationService.getById(id));
  }

  @PostMapping("/position-worktypes")
  @PreAuthorize("hasAuthority('train:position-worktype:create')")
  @AuditLog(module = "training", action = "position-worktype:create", targetType = "position-worktypes")
  public ApiResponse<TrainPositionToWorkTypeVO> create(@Valid @RequestBody TrainPositionToWorkTypeCreateReq req) {
    return ApiResponse.ok(trainPositionToWorkTypeApplicationService.create(req));
  }

  @PutMapping("/position-worktypes/{id}")
  @PreAuthorize("hasAuthority('train:position-worktype:update')")
  @AuditLog(module = "training", action = "position-worktype:update", targetType = "position-worktypes")
  public ApiResponse<TrainPositionToWorkTypeVO> update(@PathVariable Long id, @Valid @RequestBody TrainPositionToWorkTypeUpdateReq req) {
    return ApiResponse.ok(trainPositionToWorkTypeApplicationService.update(id, req));
  }

  @DeleteMapping("/position-worktypes/{id}")
  @PreAuthorize("hasAuthority('train:position-worktype:delete')")
  @AuditLog(module = "training", action = "position-worktype:delete", targetType = "position-worktypes")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainPositionToWorkTypeApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
