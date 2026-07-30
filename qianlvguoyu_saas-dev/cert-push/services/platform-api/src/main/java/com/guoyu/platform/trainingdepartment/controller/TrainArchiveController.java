package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainArchiveCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainArchiveQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainArchiveUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainArchiveApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainArchiveVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 培训档案管理接口
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainArchiveController {

  private final TrainArchiveApplicationService trainArchiveApplicationService;

  public TrainArchiveController(TrainArchiveApplicationService trainArchiveApplicationService) {
    this.trainArchiveApplicationService = trainArchiveApplicationService;
  }

  /** 分页查询 */
  @GetMapping("/archives")
  @PreAuthorize("hasAuthority('train:archive:view')")
  public ApiResponse<PageResponse<TrainArchiveVO>> page(@Valid @ModelAttribute TrainArchiveQueryReq req) {
    return ApiResponse.ok(trainArchiveApplicationService.page(req));
  }

  /** 根据ID查询 */
  @GetMapping("/archives/{id}")
  @PreAuthorize("hasAuthority('train:archive:view')")
  public ApiResponse<TrainArchiveVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainArchiveApplicationService.getById(id));
  }

  /** 创建 */
  @PostMapping("/archives")
  @PreAuthorize("hasAuthority('train:archive:create')")
  @AuditLog(module = "training", action = "archive:create", targetType = "train_archive")
  public ApiResponse<TrainArchiveVO> create(@Valid @RequestBody TrainArchiveCreateReq req) {
    return ApiResponse.ok(trainArchiveApplicationService.create(req));
  }

  /** 更新 */
  @PutMapping("/archives/{id}")
  @PreAuthorize("hasAuthority('train:archive:update')")
  @AuditLog(module = "training", action = "archive:update", targetType = "train_archive")
  public ApiResponse<TrainArchiveVO> update(@PathVariable Long id, @Valid @RequestBody TrainArchiveUpdateReq req) {
    return ApiResponse.ok(trainArchiveApplicationService.update(id, req));
  }

  /** 删除 */
  @DeleteMapping("/archives/{id}")
  @PreAuthorize("hasAuthority('train:archive:delete')")
  @AuditLog(module = "training", action = "archive:delete", targetType = "train_archive")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainArchiveApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
