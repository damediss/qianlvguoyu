package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.dto.PreparationTaskEquipmentSaveReq;
import com.guoyu.platform.certification.service.CertificationPreparationApplicationService;
import com.guoyu.platform.certification.vo.PreparationTaskEquipmentVO;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/certification/preparation")
public class CertificationPreparationEquipmentController {

  private final CertificationPreparationApplicationService service;

  public CertificationPreparationEquipmentController(CertificationPreparationApplicationService service) {
    this.service = service;
  }

  @GetMapping("/tasks/{taskId}/equipment")
  @PreAuthorize("hasAuthority('certification.preparation.equipment.view')")
  public ApiResponse<List<PreparationTaskEquipmentVO>> listTaskEquipment(@PathVariable Long taskId) {
    return ApiResponse.ok(service.listTaskEquipment(taskId));
  }

  @PostMapping("/tasks/{taskId}/equipment")
  @PreAuthorize("hasAnyAuthority('certification.preparation.equipment.bind', 'certification.preparation.equipment.assign')")
  @AuditLog(module = "certification", action = "preparation-equipment:assign", targetType = "certification_task_equipment")
  public ApiResponse<PreparationTaskEquipmentVO> assignTaskEquipment(
      @PathVariable Long taskId,
      @Valid @RequestBody PreparationTaskEquipmentSaveReq req
  ) {
    return ApiResponse.ok(service.assignTaskEquipment(taskId, req));
  }

  @PostMapping("/tasks/{taskId}/equipment/bind")
  @PreAuthorize("hasAnyAuthority('certification.preparation.equipment.bind', 'certification.preparation.equipment.assign')")
  @AuditLog(module = "certification", action = "preparation-equipment:bind", targetType = "certification_task_equipment")
  public ApiResponse<PreparationTaskEquipmentVO> bindTaskEquipment(
      @PathVariable Long taskId,
      @Valid @RequestBody PreparationTaskEquipmentSaveReq req
  ) {
    return ApiResponse.ok(service.bindTaskEquipment(taskId, req));
  }

  @DeleteMapping("/tasks/{taskId}/equipment/{taskEquipmentId}")
  @PreAuthorize("hasAnyAuthority('certification.preparation.equipment.bind', 'certification.preparation.equipment.assign')")
  @AuditLog(module = "certification", action = "preparation-equipment:remove", targetType = "certification_task_equipment")
  public ApiResponse<Void> removeTaskEquipment(@PathVariable Long taskId, @PathVariable Long taskEquipmentId) {
    service.removeTaskEquipment(taskId, taskEquipmentId);
    return ApiResponse.ok();
  }
}
