package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainResourceCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResourceQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResourceUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainResourceApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainResourceVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资源管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainResourceController {

  private final TrainResourceApplicationService trainResourceApplicationService;

  public TrainResourceController(TrainResourceApplicationService trainResourceApplicationService) {
    this.trainResourceApplicationService = trainResourceApplicationService;
  }

  /** 分页查询资源列表 */
  @GetMapping("/resources")
  @PreAuthorize("hasAuthority('train:resource:view')")
  public ApiResponse<PageResponse<TrainResourceVO>> page(@Valid @ModelAttribute TrainResourceQueryReq req) {
    return ApiResponse.ok(trainResourceApplicationService.page(req));
  }

  /** 根据ID查询资源详情 */
  @GetMapping("/resources/{id}")
  @PreAuthorize("hasAuthority('train:resource:view')")
  public ApiResponse<TrainResourceVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainResourceApplicationService.getById(id));
  }

  /** 新增资源 */
  @PostMapping("/resources")
  @PreAuthorize("hasAuthority('train:resource:create')")
  @AuditLog(module = "training", action = "resource:create", targetType = "train_resource")
  public ApiResponse<TrainResourceVO> create(@Valid @RequestBody TrainResourceCreateReq req) {
    return ApiResponse.ok(trainResourceApplicationService.create(req));
  }

  /** 修改资源 */
  @PutMapping("/resources/{id}")
  @PreAuthorize("hasAuthority('train:resource:update')")
  @AuditLog(module = "training", action = "resource:update", targetType = "train_resource")
  public ApiResponse<TrainResourceVO> update(@PathVariable Long id, @Valid @RequestBody TrainResourceUpdateReq req) {
    return ApiResponse.ok(trainResourceApplicationService.update(id, req));
  }

  /** 删除资源 */
  @DeleteMapping("/resources/{id}")
  @PreAuthorize("hasAuthority('train:resource:delete')")
  @AuditLog(module = "training", action = "resource:delete", targetType = "train_resource")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainResourceApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
