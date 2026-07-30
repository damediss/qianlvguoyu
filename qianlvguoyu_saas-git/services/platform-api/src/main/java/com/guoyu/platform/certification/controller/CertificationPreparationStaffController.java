package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.dto.PreparationTaskExamStaffSaveReq;
import com.guoyu.platform.certification.dto.PreparationTaskExamStaffRoleReq;
import com.guoyu.platform.certification.service.CertificationPreparationApplicationService;
import com.guoyu.platform.certification.vo.PreparationTaskExamStaffVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/certification/preparation")
public class CertificationPreparationStaffController {

  private final CertificationPreparationApplicationService service;

  public CertificationPreparationStaffController(CertificationPreparationApplicationService service) {
    this.service = service;
  }

  @GetMapping("/tasks/{taskId}/exam-staff")
  @PreAuthorize("hasAuthority('certification.preparation.staff.view')")
  public ApiResponse<List<PreparationTaskExamStaffVO>> listTaskExamStaff(@PathVariable Long taskId) {
    return ApiResponse.ok(service.listTaskExamStaff(taskId));
  }

  @PostMapping("/tasks/{taskId}/exam-staff")
  @PreAuthorize("hasAuthority('certification.preparation.staff.assign')")
  @AuditLog(module = "certification", action = "preparation-staff:assign", targetType = "certification_task_exam_staff")
  public ApiResponse<PreparationTaskExamStaffVO> assignExamStaff(
      @PathVariable Long taskId,
      @Valid @RequestBody PreparationTaskExamStaffSaveReq req
  ) {
    return ApiResponse.ok(service.assignExamStaff(taskId, req));
  }

  @DeleteMapping("/tasks/{taskId}/exam-staff/{taskStaffId}")
  @PreAuthorize("hasAuthority('certification.preparation.staff.assign')")
  @AuditLog(module = "certification", action = "preparation-staff:remove", targetType = "certification_task_exam_staff")
  public ApiResponse<Void> removeTaskExamStaff(@PathVariable Long taskId, @PathVariable Long taskStaffId) {
    service.removeTaskExamStaff(taskId, taskStaffId);
    return ApiResponse.ok();
  }

  @PutMapping("/tasks/{taskId}/exam-staff/{taskStaffId}/leader")
  @PreAuthorize("hasAuthority('certification.preparation.staff.assign')")
  @AuditLog(module = "certification", action = "preparation-staff:appoint-leader", targetType = "certification_task_exam_staff")
  public ApiResponse<PreparationTaskExamStaffVO> appointExamStaffLeader(
      @PathVariable Long taskId,
      @PathVariable Long taskStaffId
  ) {
    return ApiResponse.ok(service.appointExamStaffLeader(taskId, taskStaffId));
  }

  @PutMapping("/tasks/{taskId}/exam-staff/{taskStaffId}/role")
  @PreAuthorize("hasAuthority('certification.preparation.staff.assign')")
  @AuditLog(module = "certification", action = "preparation-staff:update-role", targetType = "certification_task_exam_staff")
  public ApiResponse<PreparationTaskExamStaffVO> updateExamStaffRole(
      @PathVariable Long taskId,
      @PathVariable Long taskStaffId,
      @Valid @RequestBody PreparationTaskExamStaffRoleReq req
  ) {
    return ApiResponse.ok(service.updateExamStaffRole(taskId, taskStaffId, req));
  }
}
