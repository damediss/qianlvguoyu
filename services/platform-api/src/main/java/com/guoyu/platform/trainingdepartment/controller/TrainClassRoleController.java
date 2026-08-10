package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainClassRoleAssignReq;
import com.guoyu.platform.trainingdepartment.security.TrainingAuthorityExpressions;
import com.guoyu.platform.trainingdepartment.service.TrainClassRoleApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainClassRoleVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/training")
public class TrainClassRoleController {

  private final TrainClassRoleApplicationService roleService;

  public TrainClassRoleController(TrainClassRoleApplicationService roleService) {
    this.roleService = roleService;
  }

  @GetMapping("/classes/{classId}/roles")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_VIEW)
  public ApiResponse<List<TrainClassRoleVO>> list(@PathVariable Long classId) {
    return ApiResponse.ok(roleService.listByClass(classId));
  }

  @GetMapping("/classes/{classId}/role-summary")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_VIEW
      + " or hasAuthority('train:follow:view') or hasAuthority('train:attendance:view')")
  public ApiResponse<Map<String, Object>> summary(@PathVariable Long classId) {
    return ApiResponse.ok(roleService.roleSummary(classId));
  }

  @PostMapping("/classes/{classId}/roles/assign")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_CREATE)
  @AuditLog(module = "training", action = "class-role:assign", targetType = "train_class_role")
  public ApiResponse<TrainClassRoleVO> assign(
      @PathVariable Long classId, @Valid @RequestBody TrainClassRoleAssignReq req) {
    return ApiResponse.ok(roleService.assign(classId, req));
  }

  @PostMapping("/classes/{classId}/roles/{roleId}/end")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_UPDATE)
  @AuditLog(module = "training", action = "class-role:end", targetType = "train_class_role")
  public ApiResponse<Void> end(@PathVariable Long classId, @PathVariable Long roleId) {
    roleService.endRole(classId, roleId);
    return ApiResponse.ok();
  }
}
