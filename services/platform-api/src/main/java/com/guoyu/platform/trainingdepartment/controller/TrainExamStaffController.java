package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainExamStaffCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamStaffQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamStaffUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainExamStaffApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainExamStaffVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training")
public class TrainExamStaffController {

  private final TrainExamStaffApplicationService trainExamStaffApplicationService;

  public TrainExamStaffController(TrainExamStaffApplicationService trainExamStaffApplicationService) {
    this.trainExamStaffApplicationService = trainExamStaffApplicationService;
  }

  @GetMapping("/exam-staffs")
  @PreAuthorize("hasAuthority('train:exam-staff:view')")
  public ApiResponse<PageResponse<TrainExamStaffVO>> page(@Valid @ModelAttribute TrainExamStaffQueryReq req) {
    return ApiResponse.ok(trainExamStaffApplicationService.page(req));
  }

  @GetMapping("/exam-staffs/{id}")
  @PreAuthorize("hasAuthority('train:exam-staff:view')")
  public ApiResponse<TrainExamStaffVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainExamStaffApplicationService.getById(id));
  }

  @PostMapping("/exam-staffs")
  @PreAuthorize("hasAuthority('train:exam-staff:create')")
  @AuditLog(module = "training", action = "exam-staff:create", targetType = "exam-staffs")
  public ApiResponse<TrainExamStaffVO> create(@Valid @RequestBody TrainExamStaffCreateReq req) {
    return ApiResponse.ok(trainExamStaffApplicationService.create(req));
  }

  @PutMapping("/exam-staffs/{id}")
  @PreAuthorize("hasAuthority('train:exam-staff:update')")
  @AuditLog(module = "training", action = "exam-staff:update", targetType = "exam-staffs")
  public ApiResponse<TrainExamStaffVO> update(@PathVariable Long id, @Valid @RequestBody TrainExamStaffUpdateReq req) {
    return ApiResponse.ok(trainExamStaffApplicationService.update(id, req));
  }

  @DeleteMapping("/exam-staffs/{id}")
  @PreAuthorize("hasAuthority('train:exam-staff:delete')")
  @AuditLog(module = "training", action = "exam-staff:delete", targetType = "exam-staffs")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainExamStaffApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
