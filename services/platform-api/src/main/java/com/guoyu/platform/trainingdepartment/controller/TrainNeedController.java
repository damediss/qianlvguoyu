package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainNeedCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainNeedQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainNeedUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainNeedService;
import com.guoyu.platform.trainingdepartment.vo.TrainNeedVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training")
public class TrainNeedController {

  private final TrainNeedService service;

  public TrainNeedController(TrainNeedService service) {
    this.service = service;
  }

  @GetMapping("/needs")
  @PreAuthorize("hasAuthority('train:need:view')")
  public ApiResponse<PageResponse<TrainNeedVO>> page(@Valid @ModelAttribute TrainNeedQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/needs/{id}")
  @PreAuthorize("hasAuthority('train:need:view')")
  public ApiResponse<TrainNeedVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  @PostMapping("/needs")
  @PreAuthorize("hasAuthority('train:need:create')")
  @AuditLog(module = "training", action = "need:create", targetType = "train_need")
  public ApiResponse<TrainNeedVO> create(@Valid @RequestBody TrainNeedCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  @PutMapping("/needs/{id}")
  @PreAuthorize("hasAuthority('train:need:update')")
  @AuditLog(module = "training", action = "need:update", targetType = "train_need")
  public ApiResponse<TrainNeedVO> update(@PathVariable Long id, @Valid @RequestBody TrainNeedUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  @DeleteMapping("/needs/{id}")
  @PreAuthorize("hasAuthority('train:need:delete')")
  @AuditLog(module = "training", action = "need:delete", targetType = "train_need")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
