package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainFileTempCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileTempQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileTempUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainFileTempApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainFileTempVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training")
public class TrainFileTempController {

  private final TrainFileTempApplicationService trainFileTempApplicationService;

  public TrainFileTempController(TrainFileTempApplicationService trainFileTempApplicationService) {
    this.trainFileTempApplicationService = trainFileTempApplicationService;
  }

  @GetMapping("/file-temps")
  @PreAuthorize("hasAuthority('train:file-temp:view')")
  public ApiResponse<PageResponse<TrainFileTempVO>> page(@Valid @ModelAttribute TrainFileTempQueryReq req) {
    return ApiResponse.ok(trainFileTempApplicationService.page(req));
  }

  @GetMapping("/file-temps/{id}")
  @PreAuthorize("hasAuthority('train:file-temp:view')")
  public ApiResponse<TrainFileTempVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainFileTempApplicationService.getById(id));
  }

  @PostMapping("/file-temps")
  @PreAuthorize("hasAuthority('train:file-temp:create')")
  @AuditLog(module = "training", action = "file-temp:create", targetType = "file-temps")
  public ApiResponse<TrainFileTempVO> create(@Valid @RequestBody TrainFileTempCreateReq req) {
    return ApiResponse.ok(trainFileTempApplicationService.create(req));
  }

  @PutMapping("/file-temps/{id}")
  @PreAuthorize("hasAuthority('train:file-temp:update')")
  @AuditLog(module = "training", action = "file-temp:update", targetType = "file-temps")
  public ApiResponse<TrainFileTempVO> update(@PathVariable Long id, @Valid @RequestBody TrainFileTempUpdateReq req) {
    return ApiResponse.ok(trainFileTempApplicationService.update(id, req));
  }

  @DeleteMapping("/file-temps/{id}")
  @PreAuthorize("hasAuthority('train:file-temp:delete')")
  @AuditLog(module = "training", action = "file-temp:delete", targetType = "file-temps")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainFileTempApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
