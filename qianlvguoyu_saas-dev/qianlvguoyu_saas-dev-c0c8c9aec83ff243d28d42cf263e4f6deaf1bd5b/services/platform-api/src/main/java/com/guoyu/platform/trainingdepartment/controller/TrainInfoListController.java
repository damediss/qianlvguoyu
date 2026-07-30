package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoListUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainInfoListApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainInfoListVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开班信息管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainInfoListController {

  private final TrainInfoListApplicationService trainInfoListApplicationService;

  public TrainInfoListController(TrainInfoListApplicationService trainInfoListApplicationService) {
    this.trainInfoListApplicationService = trainInfoListApplicationService;
  }

  /** 分页查询开班信息列表 */
  @GetMapping("/info-lists")
  @PreAuthorize("hasAuthority('train:info-list:view')")
  public ApiResponse<PageResponse<TrainInfoListVO>> page(@Valid @ModelAttribute TrainInfoListQueryReq req) {
    return ApiResponse.ok(trainInfoListApplicationService.page(req));
  }

  /** 根据ID查询开班信息详情 */
  @GetMapping("/info-lists/{id}")
  @PreAuthorize("hasAuthority('train:info-list:view')")
  public ApiResponse<TrainInfoListVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainInfoListApplicationService.getById(id));
  }

  /** 新增开班信息 */
  @PostMapping("/info-lists")
  @PreAuthorize("hasAuthority('train:info-list:create')")
  @AuditLog(module = "training", action = "info-list:create", targetType = "train_info_list")
  public ApiResponse<TrainInfoListVO> create(@Valid @RequestBody TrainInfoListCreateReq req) {
    return ApiResponse.ok(trainInfoListApplicationService.create(req));
  }

  /** 修改开班信息 */
  @PutMapping("/info-lists/{id}")
  @PreAuthorize("hasAuthority('train:info-list:update')")
  @AuditLog(module = "training", action = "info-list:update", targetType = "train_info_list")
  public ApiResponse<TrainInfoListVO> update(@PathVariable Long id, @Valid @RequestBody TrainInfoListUpdateReq req) {
    return ApiResponse.ok(trainInfoListApplicationService.update(id, req));
  }

  /** 删除开班信息 */
  @DeleteMapping("/info-lists/{id}")
  @PreAuthorize("hasAuthority('train:info-list:delete')")
  @AuditLog(module = "training", action = "info-list:delete", targetType = "train_info_list")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainInfoListApplicationService.delete(id);
    return ApiResponse.ok();
  }

  /** 企业确认方案 */
  @PostMapping("/info-lists/{id}/company-confirm")
  @PreAuthorize("hasAuthority('train:info-list:update')")
  @AuditLog(module = "training", action = "info-list:company-confirm", targetType = "train_info_list")
  public ApiResponse<TrainInfoListVO> companyConfirm(@PathVariable Long id) {
    return ApiResponse.ok(trainInfoListApplicationService.companyConfirm(id));
  }

  /** 企业驳回方案 */
  @PostMapping("/info-lists/{id}/company-reject")
  @PreAuthorize("hasAuthority('train:info-list:update')")
  @AuditLog(module = "training", action = "info-list:company-reject", targetType = "train_info_list")
  public ApiResponse<TrainInfoListVO> companyReject(@PathVariable Long id, @RequestParam String reason) {
    return ApiResponse.ok(trainInfoListApplicationService.companyReject(id, reason));
  }
}
