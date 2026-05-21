package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainTeacherBankCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTeacherBankQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTeacherBankUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainTeacherBankApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainTeacherBankVO;
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
 * 师资库管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainTeacherBankController {

  private final TrainTeacherBankApplicationService trainTeacherBankApplicationService;

  public TrainTeacherBankController(TrainTeacherBankApplicationService trainTeacherBankApplicationService) {
    this.trainTeacherBankApplicationService = trainTeacherBankApplicationService;
  }

  /** 分页查询师资库列表 */
  @GetMapping("/teacher-banks")
  @PreAuthorize("hasAuthority('train:teacher-bank:view')")
  public ApiResponse<PageResponse<TrainTeacherBankVO>> page(@Valid @ModelAttribute TrainTeacherBankQueryReq req) {
    return ApiResponse.ok(trainTeacherBankApplicationService.page(req));
  }

  /** 根据ID查询师资库详情 */
  @GetMapping("/teacher-banks/{id}")
  @PreAuthorize("hasAuthority('train:teacher-bank:view')")
  public ApiResponse<TrainTeacherBankVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainTeacherBankApplicationService.getById(id));
  }

  /** 新增师资 */
  @PostMapping("/teacher-banks")
  @PreAuthorize("hasAuthority('train:teacher-bank:create')")
  @AuditLog(module = "training", action = "teacher-bank:create", targetType = "train_teacher_bank")
  public ApiResponse<TrainTeacherBankVO> create(@Valid @RequestBody TrainTeacherBankCreateReq req) {
    return ApiResponse.ok(trainTeacherBankApplicationService.create(req));
  }

  /** 修改师资 */
  @PutMapping("/teacher-banks/{id}")
  @PreAuthorize("hasAuthority('train:teacher-bank:update')")
  @AuditLog(module = "training", action = "teacher-bank:update", targetType = "train_teacher_bank")
  public ApiResponse<TrainTeacherBankVO> update(@PathVariable Long id, @Valid @RequestBody TrainTeacherBankUpdateReq req) {
    return ApiResponse.ok(trainTeacherBankApplicationService.update(id, req));
  }

  /** 删除师资 */
  @DeleteMapping("/teacher-banks/{id}")
  @PreAuthorize("hasAuthority('train:teacher-bank:delete')")
  @AuditLog(module = "training", action = "teacher-bank:delete", targetType = "train_teacher_bank")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainTeacherBankApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
