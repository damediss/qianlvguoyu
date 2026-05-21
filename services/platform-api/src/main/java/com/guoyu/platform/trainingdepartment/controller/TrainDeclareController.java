package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainDeclareCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDeclareQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDeclareUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainDeclareApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainDeclareVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 培训申报管理接口
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainDeclareController {

  private final TrainDeclareApplicationService trainDeclareApplicationService;

  public TrainDeclareController(TrainDeclareApplicationService trainDeclareApplicationService) {
    this.trainDeclareApplicationService = trainDeclareApplicationService;
  }

  /** 分页查询 */
  @GetMapping("/declares")
  @PreAuthorize("hasAuthority('train:declare:view')")
  public ApiResponse<PageResponse<TrainDeclareVO>> page(@Valid @ModelAttribute TrainDeclareQueryReq req) {
    return ApiResponse.ok(trainDeclareApplicationService.page(req));
  }

  /** 根据ID查询 */
  @GetMapping("/declares/{id}")
  @PreAuthorize("hasAuthority('train:declare:view')")
  public ApiResponse<TrainDeclareVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainDeclareApplicationService.getById(id));
  }

  /** 创建 */
  @PostMapping("/declares")
  @PreAuthorize("hasAuthority('train:declare:create')")
  @AuditLog(module = "training", action = "declare:create", targetType = "train_declare")
  public ApiResponse<TrainDeclareVO> create(@Valid @RequestBody TrainDeclareCreateReq req) {
    return ApiResponse.ok(trainDeclareApplicationService.create(req));
  }

  /** 更新 */
  @PutMapping("/declares/{id}")
  @PreAuthorize("hasAuthority('train:declare:update')")
  @AuditLog(module = "training", action = "declare:update", targetType = "train_declare")
  public ApiResponse<TrainDeclareVO> update(@PathVariable Long id, @Valid @RequestBody TrainDeclareUpdateReq req) {
    return ApiResponse.ok(trainDeclareApplicationService.update(id, req));
  }

  /** 删除 */
  @DeleteMapping("/declares/{id}")
  @PreAuthorize("hasAuthority('train:declare:delete')")
  @AuditLog(module = "training", action = "declare:delete", targetType = "train_declare")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainDeclareApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
