package com.guoyu.platform.industrial.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.industrial.dto.IndustrialCollegeCreateReq;
import com.guoyu.platform.industrial.dto.IndustrialCollegeQueryReq;
import com.guoyu.platform.industrial.dto.IndustrialCollegeUpdateReq;
import com.guoyu.platform.industrial.service.IndustrialCollegeApplicationService;
import com.guoyu.platform.industrial.vo.IndustrialCollegeStatsVO;
import com.guoyu.platform.industrial.vo.IndustrialCollegeVO;
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

@Validated
@RestController
@RequestMapping("/industrial/college")
public class IndustrialCollegeController {

  private final IndustrialCollegeApplicationService service;

  public IndustrialCollegeController(IndustrialCollegeApplicationService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('industrial:college:view')")
  public ApiResponse<PageResponse<IndustrialCollegeVO>> page(@Valid @ModelAttribute IndustrialCollegeQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/stats")
  @PreAuthorize("hasAuthority('industrial:college:view')")
  public ApiResponse<IndustrialCollegeStatsVO> stats() {
    return ApiResponse.ok(service.stats());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('industrial:college:view')")
  public ApiResponse<IndustrialCollegeVO> get(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('industrial:college:create')")
  @AuditLog(module = "industrial", action = "college:create", targetType = "industrial_college")
  public ApiResponse<IndustrialCollegeVO> create(@Valid @RequestBody IndustrialCollegeCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('industrial:college:update')")
  @AuditLog(module = "industrial", action = "college:update", targetType = "industrial_college")
  public ApiResponse<IndustrialCollegeVO> update(@PathVariable Long id, @Valid @RequestBody IndustrialCollegeUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('industrial:college:delete')")
  @AuditLog(module = "industrial", action = "college:delete", targetType = "industrial_college")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
