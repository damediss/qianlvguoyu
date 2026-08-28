package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.SystemCompanyCreateReq;
import com.guoyu.platform.trainingdepartment.dto.SystemCompanyUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainSystemCompanyApplicationService;
import com.guoyu.platform.trainingdepartment.vo.SystemCompanyVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 培训管理 · 支持保障 · 企业管理（系统企业 company）。
 */
@Validated
@RestController
@RequestMapping("/training/system-companies")
public class TrainSystemCompanyController {

  private final TrainSystemCompanyApplicationService service;

  public TrainSystemCompanyController(TrainSystemCompanyApplicationService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('train:company:view')")
  public ApiResponse<List<SystemCompanyVO>> list(@RequestParam(required = false) String keyword) {
    return ApiResponse.ok(service.list(keyword));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('train:company:create')")
  @AuditLog(module = "training", action = "company:create", targetType = "sys_org")
  public ApiResponse<SystemCompanyVO> create(@Valid @RequestBody SystemCompanyCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('train:company:update')")
  @AuditLog(module = "training", action = "company:update", targetType = "sys_org")
  public ApiResponse<SystemCompanyVO> update(
      @PathVariable Long id, @Valid @RequestBody SystemCompanyUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('train:company:delete')")
  @AuditLog(module = "training", action = "company:delete", targetType = "sys_org")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
