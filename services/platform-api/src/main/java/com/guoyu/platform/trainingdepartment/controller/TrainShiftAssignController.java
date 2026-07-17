package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainShiftAssignBindClassReq;
import com.guoyu.platform.trainingdepartment.dto.TrainShiftAssignGenerateMaterializeReq;
import com.guoyu.platform.trainingdepartment.dto.TrainShiftAssignGenerateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainShiftAssignMaterializeReq;
import com.guoyu.platform.trainingdepartment.dto.TrainShiftAssignPatchReq;
import com.guoyu.platform.trainingdepartment.security.TrainingAuthorityExpressions;
import com.guoyu.platform.trainingdepartment.service.shiftassign.ShiftAssignApplicationService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/training/shift-assign")
public class TrainShiftAssignController {

  private final ShiftAssignApplicationService shiftAssignService;

  public TrainShiftAssignController(ShiftAssignApplicationService shiftAssignService) {
    this.shiftAssignService = shiftAssignService;
  }

  @PostMapping("/adapters/preview")
  @PreAuthorize(TrainingAuthorityExpressions.DIVISION_VIEW)
  public ApiResponse<Map<String, Object>> preview(@Valid @RequestBody TrainShiftAssignGenerateReq req) {
    return ApiResponse.ok(shiftAssignService.previewAdapter(req));
  }

  @PostMapping("/precheck")
  @PreAuthorize(TrainingAuthorityExpressions.DIVISION_VIEW)
  public ApiResponse<Map<String, Object>> precheck(@Valid @RequestBody TrainShiftAssignGenerateReq req) {
    return ApiResponse.ok(shiftAssignService.precheck(req));
  }

  @PostMapping("/generate")
  @PreAuthorize(TrainingAuthorityExpressions.DIVISION_MANAGE)
  @AuditLog(module = "training", action = "shift-assign:generate", targetType = "train_shift_assign_task")
  public ApiResponse<Map<String, Object>> generate(@Valid @RequestBody TrainShiftAssignGenerateReq req) {
    return ApiResponse.ok(shiftAssignService.generate(req));
  }

  @PostMapping("/generate-and-materialize")
  @PreAuthorize(TrainingAuthorityExpressions.DIVISION_MANAGE)
  @AuditLog(module = "training", action = "shift-assign:generate-materialize", targetType = "train_shift_assign_task")
  public ApiResponse<Map<String, Object>> generateAndMaterialize(
      @Valid @RequestBody TrainShiftAssignGenerateMaterializeReq req) {
    return ApiResponse.ok(shiftAssignService.generateAndMaterialize(req));
  }

  @GetMapping("/tasks/{id}")
  @PreAuthorize(TrainingAuthorityExpressions.DIVISION_VIEW)
  public ApiResponse<Map<String, Object>> getTask(@PathVariable Long id) {
    return ApiResponse.ok(shiftAssignService.getTask(id));
  }

  @PostMapping("/tasks/{id}/materialize")
  @PreAuthorize(TrainingAuthorityExpressions.DIVISION_MANAGE)
  @AuditLog(module = "training", action = "shift-assign:materialize", targetType = "train_shift_assign_task")
  public ApiResponse<Map<String, Object>> materialize(
      @PathVariable Long id,
      @Valid @RequestBody TrainShiftAssignMaterializeReq req) {
    return ApiResponse.ok(shiftAssignService.materialize(id, req));
  }

  @PostMapping("/tasks/{id}/patch")
  @PreAuthorize(TrainingAuthorityExpressions.DIVISION_MANAGE)
  @AuditLog(module = "training", action = "shift-assign:patch", targetType = "train_shift_assign_task")
  public ApiResponse<Map<String, Object>> patch(
      @PathVariable Long id,
      @Valid @RequestBody TrainShiftAssignPatchReq req) {
    return ApiResponse.ok(shiftAssignService.patch(id, req));
  }

  @PostMapping("/tasks/{id}/bind-class")
  @PreAuthorize(TrainingAuthorityExpressions.DIVISION_MANAGE)
  @AuditLog(module = "training", action = "shift-assign:bind-class", targetType = "train_shift_assign_task")
  public ApiResponse<Map<String, Object>> bindClass(
      @PathVariable Long id,
      @Valid @RequestBody TrainShiftAssignBindClassReq req) {
    return ApiResponse.ok(shiftAssignService.bindClass(id, req));
  }
}
