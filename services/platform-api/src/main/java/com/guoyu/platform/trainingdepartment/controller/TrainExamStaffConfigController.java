package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainExamStaffConfigCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamStaffConfigQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamStaffConfigUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainExamStaffConfigApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainExamStaffConfigVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training")
public class TrainExamStaffConfigController {

  private final TrainExamStaffConfigApplicationService trainExamStaffConfigApplicationService;

  public TrainExamStaffConfigController(TrainExamStaffConfigApplicationService trainExamStaffConfigApplicationService) {
    this.trainExamStaffConfigApplicationService = trainExamStaffConfigApplicationService;
  }

  @GetMapping("/exam-staff-configs")
  @PreAuthorize("hasAuthority('train:exam-staff-config:view')")
  public ApiResponse<PageResponse<TrainExamStaffConfigVO>> page(@Valid @ModelAttribute TrainExamStaffConfigQueryReq req) {
    return ApiResponse.ok(trainExamStaffConfigApplicationService.page(req));
  }

  @GetMapping("/exam-staff-configs/{id}")
  @PreAuthorize("hasAuthority('train:exam-staff-config:view')")
  public ApiResponse<TrainExamStaffConfigVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainExamStaffConfigApplicationService.getById(id));
  }

  @PostMapping("/exam-staff-configs")
  @PreAuthorize("hasAuthority('train:exam-staff-config:create')")
  @AuditLog(module = "training", action = "exam-staff-config:create", targetType = "exam-staff-configs")
  public ApiResponse<TrainExamStaffConfigVO> create(@Valid @RequestBody TrainExamStaffConfigCreateReq req) {
    return ApiResponse.ok(trainExamStaffConfigApplicationService.create(req));
  }

  @PutMapping("/exam-staff-configs/{id}")
  @PreAuthorize("hasAuthority('train:exam-staff-config:update')")
  @AuditLog(module = "training", action = "exam-staff-config:update", targetType = "exam-staff-configs")
  public ApiResponse<TrainExamStaffConfigVO> update(@PathVariable Long id, @Valid @RequestBody TrainExamStaffConfigUpdateReq req) {
    return ApiResponse.ok(trainExamStaffConfigApplicationService.update(id, req));
  }

  @DeleteMapping("/exam-staff-configs/{id}")
  @PreAuthorize("hasAuthority('train:exam-staff-config:delete')")
  @AuditLog(module = "training", action = "exam-staff-config:delete", targetType = "exam-staff-configs")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainExamStaffConfigApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
