package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainUserLearnLogCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserLearnLogQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserLearnLogUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainUserLearnLogApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainUserLearnLogVO;
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
 * 学习记录管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainUserLearnLogController {

  private final TrainUserLearnLogApplicationService trainUserLearnLogApplicationService;

  public TrainUserLearnLogController(TrainUserLearnLogApplicationService trainUserLearnLogApplicationService) {
    this.trainUserLearnLogApplicationService = trainUserLearnLogApplicationService;
  }

  /** 分页查询学习记录列表 */
  @GetMapping("/user-learn-logs")
  @PreAuthorize("hasAuthority('train:user-learn-log:view')")
  public ApiResponse<PageResponse<TrainUserLearnLogVO>> page(@Valid @ModelAttribute TrainUserLearnLogQueryReq req) {
    return ApiResponse.ok(trainUserLearnLogApplicationService.page(req));
  }

  /** 根据ID查询学习记录详情 */
  @GetMapping("/user-learn-logs/{id}")
  @PreAuthorize("hasAuthority('train:user-learn-log:view')")
  public ApiResponse<TrainUserLearnLogVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainUserLearnLogApplicationService.getById(id));
  }

  /** 新增学习记录 */
  @PostMapping("/user-learn-logs")
  @PreAuthorize("hasAuthority('train:user-learn-log:create')")
  @AuditLog(module = "training", action = "user-learn-log:create", targetType = "train_user_learn_log")
  public ApiResponse<TrainUserLearnLogVO> create(@Valid @RequestBody TrainUserLearnLogCreateReq req) {
    return ApiResponse.ok(trainUserLearnLogApplicationService.create(req));
  }

  /** 修改学习记录 */
  @PutMapping("/user-learn-logs/{id}")
  @PreAuthorize("hasAuthority('train:user-learn-log:update')")
  @AuditLog(module = "training", action = "user-learn-log:update", targetType = "train_user_learn_log")
  public ApiResponse<TrainUserLearnLogVO> update(@PathVariable Long id, @Valid @RequestBody TrainUserLearnLogUpdateReq req) {
    return ApiResponse.ok(trainUserLearnLogApplicationService.update(id, req));
  }

  /** 删除学习记录 */
  @DeleteMapping("/user-learn-logs/{id}")
  @PreAuthorize("hasAuthority('train:user-learn-log:delete')")
  @AuditLog(module = "training", action = "user-learn-log:delete", targetType = "train_user_learn_log")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainUserLearnLogApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
