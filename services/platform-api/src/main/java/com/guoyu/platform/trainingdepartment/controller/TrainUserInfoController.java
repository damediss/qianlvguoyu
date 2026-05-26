package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainUserInfoCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserInfoQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserInfoUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainUserInfoApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainUserInfoVO;
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
 * 参与培训人员信息管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainUserInfoController {

  private final TrainUserInfoApplicationService trainUserInfoApplicationService;

  public TrainUserInfoController(TrainUserInfoApplicationService trainUserInfoApplicationService) {
    this.trainUserInfoApplicationService = trainUserInfoApplicationService;
  }

  /** 分页查询参与培训人员信息列表 */
  @GetMapping("/user-infos")
  @PreAuthorize("hasAuthority('train:user-info:view')")
  public ApiResponse<PageResponse<TrainUserInfoVO>> page(@Valid @ModelAttribute TrainUserInfoQueryReq req) {
    return ApiResponse.ok(trainUserInfoApplicationService.page(req));
  }

  /** 根据ID查询参与培训人员信息详情 */
  @GetMapping("/user-infos/{id}")
  @PreAuthorize("hasAuthority('train:user-info:view')")
  public ApiResponse<TrainUserInfoVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainUserInfoApplicationService.getById(id));
  }

  /** 新增参与培训人员信息 */
  @PostMapping("/user-infos")
  @PreAuthorize("hasAuthority('train:user-info:create')")
  @AuditLog(module = "training", action = "user-info:create", targetType = "train_user_info")
  public ApiResponse<TrainUserInfoVO> create(@Valid @RequestBody TrainUserInfoCreateReq req) {
    return ApiResponse.ok(trainUserInfoApplicationService.create(req));
  }

  /** 修改参与培训人员信息 */
  @PutMapping("/user-infos/{id}")
  @PreAuthorize("hasAuthority('train:user-info:update')")
  @AuditLog(module = "training", action = "user-info:update", targetType = "train_user_info")
  public ApiResponse<TrainUserInfoVO> update(@PathVariable Long id, @Valid @RequestBody TrainUserInfoUpdateReq req) {
    return ApiResponse.ok(trainUserInfoApplicationService.update(id, req));
  }

  /** 删除参与培训人员信息 */
  @DeleteMapping("/user-infos/{id}")
  @PreAuthorize("hasAuthority('train:user-info:delete')")
  @AuditLog(module = "training", action = "user-info:delete", targetType = "train_user_info")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainUserInfoApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
