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

@Validated
@RestController
@RequestMapping("/training")
public class TrainSolutionController {

  private final TrainSolutionService service;

  public TrainSolutionController(TrainSolutionService service) {
    this.service = service;
  }

  @GetMapping("/solutions")
  @PreAuthorize("hasAuthority('train:solution:view')")
  public ApiResponse<PageResponse<TrainSolutionVO>> page(@Valid @ModelAttribute TrainSolutionQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/solutions/{id}")
  @PreAuthorize("hasAuthority('train:solution:view')")
  public ApiResponse<TrainSolutionVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  @PostMapping("/solutions")
  @PreAuthorize("hasAuthority('train:solution:create')")
  @AuditLog(module = "training", action = "solution:create", targetType = "train_solution")
  public ApiResponse<TrainSolutionVO> create(@Valid @RequestBody TrainSolutionCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  @PutMapping("/solutions/{id}")
  @PreAuthorize("hasAuthority('train:solution:update')")
  @AuditLog(module = "training", action = "solution:update", targetType = "train_solution")
  public ApiResponse<TrainSolutionVO> update(@PathVariable Long id, @Valid @RequestBody TrainSolutionUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  @DeleteMapping("/solutions/{id}")
  @PreAuthorize("hasAuthority('train:solution:delete')")
  @AuditLog(module = "training", action = "solution:delete", targetType = "train_solution")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
