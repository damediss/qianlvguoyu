package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.DemandEmployeeCreateReq;
import com.guoyu.platform.trainingdepartment.dto.DemandEmployeeQueryReq;
import com.guoyu.platform.trainingdepartment.dto.DemandEmployeeUpdateReq;
import com.guoyu.platform.trainingdepartment.service.DemandEmployeeApplicationService;
import com.guoyu.platform.trainingdepartment.vo.DemandEmployeeVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training")
public class DemandEmployeeController {

  private final DemandEmployeeApplicationService demandEmployeeApplicationService;

  public DemandEmployeeController(DemandEmployeeApplicationService demandEmployeeApplicationService) {
    this.demandEmployeeApplicationService = demandEmployeeApplicationService;
  }

  @GetMapping("/demand-employees")
  @PreAuthorize("hasAuthority('train:demand-employee:view')")
  public ApiResponse<PageResponse<DemandEmployeeVO>> page(@Valid @ModelAttribute DemandEmployeeQueryReq req) {
    return ApiResponse.ok(demandEmployeeApplicationService.page(req));
  }

  @GetMapping("/demand-employees/{id}")
  @PreAuthorize("hasAuthority('train:demand-employee:view')")
  public ApiResponse<DemandEmployeeVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(demandEmployeeApplicationService.getById(id));
  }

  @PostMapping("/demand-employees")
  @PreAuthorize("hasAuthority('train:demand-employee:create')")
  @AuditLog(module = "training", action = "demand-employee:create", targetType = "demand-employees")
  public ApiResponse<DemandEmployeeVO> create(@Valid @RequestBody DemandEmployeeCreateReq req) {
    return ApiResponse.ok(demandEmployeeApplicationService.create(req));
  }

  @PutMapping("/demand-employees/{id}")
  @PreAuthorize("hasAuthority('train:demand-employee:update')")
  @AuditLog(module = "training", action = "demand-employee:update", targetType = "demand-employees")
  public ApiResponse<DemandEmployeeVO> update(@PathVariable Long id, @Valid @RequestBody DemandEmployeeUpdateReq req) {
    return ApiResponse.ok(demandEmployeeApplicationService.update(id, req));
  }

  @DeleteMapping("/demand-employees/{id}")
  @PreAuthorize("hasAuthority('train:demand-employee:delete')")
  @AuditLog(module = "training", action = "demand-employee:delete", targetType = "demand-employees")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    demandEmployeeApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
