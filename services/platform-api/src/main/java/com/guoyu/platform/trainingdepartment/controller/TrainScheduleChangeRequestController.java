package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.ScheduleChangeRejectReq;
import com.guoyu.platform.trainingdepartment.service.TrainScheduleChangeRequestApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainScheduleChangeRequestVO;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端调课审批 —— 《App课表与调课审批实现方案》S2。
 */
@Validated
@RestController
@RequestMapping("/training/schedule-change-requests")
public class TrainScheduleChangeRequestController {

  private final TrainScheduleChangeRequestApplicationService service;

  public TrainScheduleChangeRequestController(TrainScheduleChangeRequestApplicationService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<List<TrainScheduleChangeRequestVO>> list(
      @RequestParam(required = false) Long classId,
      @RequestParam(required = false) String status) {
    return ApiResponse.ok(service.listAdmin(classId, status));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<TrainScheduleChangeRequestVO> detail(@PathVariable Long id) {
    return ApiResponse.ok(service.getAdmin(id));
  }

  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "schedule-change:approve", targetType = "train_schedule_change_request")
  public ApiResponse<TrainScheduleChangeRequestVO> approve(
      @PathVariable Long id,
      @RequestParam(defaultValue = "false") boolean force) {
    return ApiResponse.ok(service.approve(id, force));
  }

  @PostMapping("/{id}/reject")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "schedule-change:reject", targetType = "train_schedule_change_request")
  public ApiResponse<TrainScheduleChangeRequestVO> reject(
      @PathVariable Long id,
      @RequestBody(required = false) ScheduleChangeRejectReq body) {
    return ApiResponse.ok(service.reject(id, body));
  }
}
