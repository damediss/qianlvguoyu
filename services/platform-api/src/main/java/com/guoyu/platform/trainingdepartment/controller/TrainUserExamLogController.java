package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainUserExamLogCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserExamLogQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserExamLogUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainUserExamLogApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainUserExamLogVO;
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
 * 用户刷题记录管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainUserExamLogController {

  private final TrainUserExamLogApplicationService trainUserExamLogApplicationService;

  public TrainUserExamLogController(TrainUserExamLogApplicationService trainUserExamLogApplicationService) {
    this.trainUserExamLogApplicationService = trainUserExamLogApplicationService;
  }

  /** 分页查询用户刷题记录列表 */
  @GetMapping("/user-exam-logs")
  @PreAuthorize("hasAuthority('train:user-exam-log:view')")
  public ApiResponse<PageResponse<TrainUserExamLogVO>> page(@Valid @ModelAttribute TrainUserExamLogQueryReq req) {
    return ApiResponse.ok(trainUserExamLogApplicationService.page(req));
  }

  /** 根据ID查询用户刷题记录详情 */
  @GetMapping("/user-exam-logs/{id}")
  @PreAuthorize("hasAuthority('train:user-exam-log:view')")
  public ApiResponse<TrainUserExamLogVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainUserExamLogApplicationService.getById(id));
  }

  /** 新增用户刷题记录 */
  @PostMapping("/user-exam-logs")
  @PreAuthorize("hasAuthority('train:user-exam-log:create')")
  @AuditLog(module = "training", action = "user-exam-log:create", targetType = "train_user_exam_log")
  public ApiResponse<TrainUserExamLogVO> create(@Valid @RequestBody TrainUserExamLogCreateReq req) {
    return ApiResponse.ok(trainUserExamLogApplicationService.create(req));
  }

  /** 修改用户刷题记录 */
  @PutMapping("/user-exam-logs/{id}")
  @PreAuthorize("hasAuthority('train:user-exam-log:update')")
  @AuditLog(module = "training", action = "user-exam-log:update", targetType = "train_user_exam_log")
  public ApiResponse<TrainUserExamLogVO> update(@PathVariable Long id, @Valid @RequestBody TrainUserExamLogUpdateReq req) {
    return ApiResponse.ok(trainUserExamLogApplicationService.update(id, req));
  }

  /** 删除用户刷题记录 */
  @DeleteMapping("/user-exam-logs/{id}")
  @PreAuthorize("hasAuthority('train:user-exam-log:delete')")
  @AuditLog(module = "training", action = "user-exam-log:delete", targetType = "train_user_exam_log")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainUserExamLogApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
