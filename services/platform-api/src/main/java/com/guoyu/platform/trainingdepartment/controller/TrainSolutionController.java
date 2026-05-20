package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainSolutionService;
import com.guoyu.platform.trainingdepartment.vo.TrainSolutionVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 培训方案管理接口
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainSolutionController {

  private final TrainSolutionService service;

  public TrainSolutionController(TrainSolutionService service) {
    this.service = service;
  }

  /** 分页查询 */
  @GetMapping("/solutions")
  @PreAuthorize("hasAuthority('train:solution:view')")
  public ApiResponse<PageResponse<TrainSolutionVO>> page(@Valid @ModelAttribute TrainSolutionQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  /** 根据ID查询 */
  @GetMapping("/solutions/{id}")
  @PreAuthorize("hasAuthority('train:solution:view')")
  public ApiResponse<TrainSolutionVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  /** 创建 */
  @PostMapping("/solutions")
  @PreAuthorize("hasAuthority('train:solution:create')")
  @AuditLog(module = "training", action = "solution:create", targetType = "train_solution")
  public ApiResponse<TrainSolutionVO> create(@Valid @RequestBody TrainSolutionCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  /** 更新 */
  @PutMapping("/solutions/{id}")
  @PreAuthorize("hasAuthority('train:solution:update')")
  @AuditLog(module = "training", action = "solution:update", targetType = "train_solution")
  public ApiResponse<TrainSolutionVO> update(@PathVariable Long id, @Valid @RequestBody TrainSolutionUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  /** 删除 */
  @DeleteMapping("/solutions/{id}")
  @PreAuthorize("hasAuthority('train:solution:delete')")
  @AuditLog(module = "training", action = "solution:delete", targetType = "train_solution")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }

  /** 提交审批 */
  @PostMapping("/solutions/{id}/submit")
  @PreAuthorize("hasAuthority('train:solution:update')")
  @AuditLog(module = "training", action = "solution:submit", targetType = "train_solution")
  public ApiResponse<TrainSolutionVO> submit(@PathVariable Long id) {
    return ApiResponse.ok(service.submit(id));
  }

  /** 审批通过 */
  @PostMapping("/solutions/{id}/approve")
  @PreAuthorize("hasAuthority('train:solution:update')")
  @AuditLog(module = "training", action = "solution:approve", targetType = "train_solution")
  public ApiResponse<TrainSolutionVO> approve(@PathVariable Long id) {
    return ApiResponse.ok(service.approve(id));
  }

  /** 驳回 */
  @PostMapping("/solutions/{id}/reject")
  @PreAuthorize("hasAuthority('train:solution:update')")
  @AuditLog(module = "training", action = "solution:reject", targetType = "train_solution")
  public ApiResponse<TrainSolutionVO> reject(@PathVariable Long id, @RequestParam String reason) {
    return ApiResponse.ok(service.reject(id, reason));
  }
}
