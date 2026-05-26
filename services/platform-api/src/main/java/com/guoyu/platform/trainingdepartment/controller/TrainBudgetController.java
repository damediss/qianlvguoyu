package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainBudgetCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainBudgetQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainBudgetUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainBudgetApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainBudgetVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 培训预算管理接口
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainBudgetController {

  private final TrainBudgetApplicationService trainBudgetApplicationService;

  public TrainBudgetController(TrainBudgetApplicationService trainBudgetApplicationService) {
    this.trainBudgetApplicationService = trainBudgetApplicationService;
  }

  /** 分页查询培训预算 */
  @GetMapping("/budgets")
  @PreAuthorize("hasAuthority('train:budget:view')")
  public ApiResponse<PageResponse<TrainBudgetVO>> page(@Valid @ModelAttribute TrainBudgetQueryReq req) {
    return ApiResponse.ok(trainBudgetApplicationService.page(req));
  }

  /** 根据ID查询培训预算 */
  @GetMapping("/budgets/{id}")
  @PreAuthorize("hasAuthority('train:budget:view')")
  public ApiResponse<TrainBudgetVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainBudgetApplicationService.getById(id));
  }

  /** 创建培训预算 */
  @PostMapping("/budgets")
  @PreAuthorize("hasAuthority('train:budget:create')")
  @AuditLog(module = "training", action = "budget:create", targetType = "train_budget")
  public ApiResponse<TrainBudgetVO> create(@Valid @RequestBody TrainBudgetCreateReq req) {
    return ApiResponse.ok(trainBudgetApplicationService.create(req));
  }

  /** 更新培训预算 */
  @PutMapping("/budgets/{id}")
  @PreAuthorize("hasAuthority('train:budget:update')")
  @AuditLog(module = "training", action = "budget:update", targetType = "train_budget")
  public ApiResponse<TrainBudgetVO> update(@PathVariable Long id, @Valid @RequestBody TrainBudgetUpdateReq req) {
    return ApiResponse.ok(trainBudgetApplicationService.update(id, req));
  }

  /** 删除培训预算 */
  @DeleteMapping("/budgets/{id}")
  @PreAuthorize("hasAuthority('train:budget:delete')")
  @AuditLog(module = "training", action = "budget:delete", targetType = "train_budget")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainBudgetApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
