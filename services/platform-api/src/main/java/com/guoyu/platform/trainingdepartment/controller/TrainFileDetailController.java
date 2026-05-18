package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainFileDetailCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileDetailQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileDetailUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainFileDetailService;
import com.guoyu.platform.trainingdepartment.vo.TrainFileDetailVO;
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

/**
 * 文件详情管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainFileDetailController {

  private final TrainFileDetailService service;

  public TrainFileDetailController(TrainFileDetailService service) {
    this.service = service;
  }

  /** 分页查询文件详情列表 */
  @GetMapping("/file-details")
  @PreAuthorize("hasAuthority('train:file-detail:view')")
  public ApiResponse<PageResponse<TrainFileDetailVO>> page(@Valid @ModelAttribute TrainFileDetailQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  /** 根据ID查询文件详情 */
  @GetMapping("/file-details/{id}")
  @PreAuthorize("hasAuthority('train:file-detail:view')")
  public ApiResponse<TrainFileDetailVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  /** 新增文件详情 */
  @PostMapping("/file-details")
  @PreAuthorize("hasAuthority('train:file-detail:create')")
  @AuditLog(module = "training", action = "file-detail:create", targetType = "train_file_detail")
  public ApiResponse<TrainFileDetailVO> create(@Valid @RequestBody TrainFileDetailCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  /** 修改文件详情 */
  @PutMapping("/file-details/{id}")
  @PreAuthorize("hasAuthority('train:file-detail:update')")
  @AuditLog(module = "training", action = "file-detail:update", targetType = "train_file_detail")
  public ApiResponse<TrainFileDetailVO> update(@PathVariable Long id, @Valid @RequestBody TrainFileDetailUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  /** 删除文件详情 */
  @DeleteMapping("/file-details/{id}")
  @PreAuthorize("hasAuthority('train:file-detail:delete')")
  @AuditLog(module = "training", action = "file-detail:delete", targetType = "train_file_detail")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
