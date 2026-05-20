package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainSummaryCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSummaryQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSummaryUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainSummaryService;
import com.guoyu.platform.trainingdepartment.vo.TrainSummaryVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 培训总结管理接口
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainSummaryController {

  private final TrainSummaryService service;

  public TrainSummaryController(TrainSummaryService service) {
    this.service = service;
  }

  /** 分页查询 */
  @GetMapping("/summaries")
  @PreAuthorize("hasAuthority('train:summary:view')")
  public ApiResponse<PageResponse<TrainSummaryVO>> page(@Valid @ModelAttribute TrainSummaryQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  /** 根据ID查询 */
  @GetMapping("/summaries/{id}")
  @PreAuthorize("hasAuthority('train:summary:view')")
  public ApiResponse<TrainSummaryVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  /** 创建 */
  @PostMapping("/summaries")
  @PreAuthorize("hasAuthority('train:summary:create')")
  @AuditLog(module = "training", action = "summary:create", targetType = "train_summary")
  public ApiResponse<TrainSummaryVO> create(@Valid @RequestBody TrainSummaryCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  /** 更新 */
  @PutMapping("/summaries/{id}")
  @PreAuthorize("hasAuthority('train:summary:update')")
  @AuditLog(module = "training", action = "summary:update", targetType = "train_summary")
  public ApiResponse<TrainSummaryVO> update(@PathVariable Long id, @Valid @RequestBody TrainSummaryUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  /** 删除 */
  @DeleteMapping("/summaries/{id}")
  @PreAuthorize("hasAuthority('train:summary:delete')")
  @AuditLog(module = "training", action = "summary:delete", targetType = "train_summary")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
