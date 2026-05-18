package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainBudgetCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainBudgetQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainBudgetUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainBudgetService;
import com.guoyu.platform.trainingdepartment.vo.TrainBudgetVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training")
public class TrainBudgetController {

  private final TrainBudgetService service;

  public TrainBudgetController(TrainBudgetService service) {
    this.service = service;
  }

  @GetMapping("/budgets")
  @PreAuthorize("hasAuthority('train:budget:view')")
  public ApiResponse<PageResponse<TrainBudgetVO>> page(@Valid @ModelAttribute TrainBudgetQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/budgets/{id}")
  @PreAuthorize("hasAuthority('train:budget:view')")
  public ApiResponse<TrainBudgetVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  @PostMapping("/budgets")
  @PreAuthorize("hasAuthority('train:budget:create')")
  @AuditLog(module = "training", action = "budget:create", targetType = "train_budget")
  public ApiResponse<TrainBudgetVO> create(@Valid @RequestBody TrainBudgetCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  @PutMapping("/budgets/{id}")
  @PreAuthorize("hasAuthority('train:budget:update')")
  @AuditLog(module = "training", action = "budget:update", targetType = "train_budget")
  public ApiResponse<TrainBudgetVO> update(@PathVariable Long id, @Valid @RequestBody TrainBudgetUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  @DeleteMapping("/budgets/{id}")
  @PreAuthorize("hasAuthority('train:budget:delete')")
  @AuditLog(module = "training", action = "budget:delete", targetType = "train_budget")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
