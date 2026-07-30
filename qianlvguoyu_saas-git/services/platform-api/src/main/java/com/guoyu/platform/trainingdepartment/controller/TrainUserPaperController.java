package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainUserPaperCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserPaperQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserPaperUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainUserPaperApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainUserPaperVO;
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
 * 用户证书管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainUserPaperController {

  private final TrainUserPaperApplicationService trainUserPaperApplicationService;

  public TrainUserPaperController(TrainUserPaperApplicationService trainUserPaperApplicationService) {
    this.trainUserPaperApplicationService = trainUserPaperApplicationService;
  }

  /** 分页查询用户证书列表 */
  @GetMapping("/user-papers")
  @PreAuthorize("hasAuthority('train:user-paper:view')")
  public ApiResponse<PageResponse<TrainUserPaperVO>> page(@Valid @ModelAttribute TrainUserPaperQueryReq req) {
    return ApiResponse.ok(trainUserPaperApplicationService.page(req));
  }

  /** 根据ID查询用户证书详情 */
  @GetMapping("/user-papers/{id}")
  @PreAuthorize("hasAuthority('train:user-paper:view')")
  public ApiResponse<TrainUserPaperVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainUserPaperApplicationService.getById(id));
  }

  /** 新增用户证书 */
  @PostMapping("/user-papers")
  @PreAuthorize("hasAuthority('train:user-paper:create')")
  @AuditLog(module = "training", action = "user-paper:create", targetType = "train_user_paper")
  public ApiResponse<TrainUserPaperVO> create(@Valid @RequestBody TrainUserPaperCreateReq req) {
    return ApiResponse.ok(trainUserPaperApplicationService.create(req));
  }

  /** 修改用户证书 */
  @PutMapping("/user-papers/{id}")
  @PreAuthorize("hasAuthority('train:user-paper:update')")
  @AuditLog(module = "training", action = "user-paper:update", targetType = "train_user_paper")
  public ApiResponse<TrainUserPaperVO> update(@PathVariable Long id, @Valid @RequestBody TrainUserPaperUpdateReq req) {
    return ApiResponse.ok(trainUserPaperApplicationService.update(id, req));
  }

  /** 删除用户证书 */
  @DeleteMapping("/user-papers/{id}")
  @PreAuthorize("hasAuthority('train:user-paper:delete')")
  @AuditLog(module = "training", action = "user-paper:delete", targetType = "train_user_paper")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainUserPaperApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
