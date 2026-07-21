package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainDivisionAutoPackReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDivisionEnsureTrainInfoReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDivisionOrgSplitReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDivisionPackPatchReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDivisionRosterParseReq;
import com.guoyu.platform.trainingdepartment.security.TrainingAuthorityExpressions;
import com.guoyu.platform.trainingdepartment.service.TrainDivisionApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainDivisionPlanVO;
import com.guoyu.platform.trainingdepartment.vo.TrainDivisionReadyVO;
import com.guoyu.platform.trainingdepartment.vo.TrainPlanRosterVO;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/training/division")
public class TrainDivisionController {

  private final TrainDivisionApplicationService divisionService;

  public TrainDivisionController(TrainDivisionApplicationService divisionService) {
    this.divisionService = divisionService;
  }

  @GetMapping("/plans")
  @PreAuthorize(TrainingAuthorityExpressions.DIVISION_VIEW)
  public ApiResponse<PageResponse<TrainDivisionPlanVO>> pagePlans(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int pageSize,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false, defaultValue = "false") Boolean readyOnly) {
    return ApiResponse.ok(divisionService.pagePlans(page, pageSize, keyword, readyOnly));
  }

  @GetMapping("/plans/{planId}/ready")
  @PreAuthorize(TrainingAuthorityExpressions.DIVISION_VIEW)
  public ApiResponse<TrainDivisionReadyVO> ready(@PathVariable Long planId) {
    return ApiResponse.ok(divisionService.ready(planId));
  }

  @PostMapping("/plans/{planId}/ensure-train-info")
  @PreAuthorize(TrainingAuthorityExpressions.DIVISION_MANAGE)
  @AuditLog(module = "training", action = "division:ensure-train-info", targetType = "train_info_list")
  public ApiResponse<Map<String, Object>> ensureTrainInfo(
      @PathVariable Long planId,
      @Valid @RequestBody(required = false) TrainDivisionEnsureTrainInfoReq req) {
    if (req == null) req = new TrainDivisionEnsureTrainInfoReq();
    return ApiResponse.ok(divisionService.ensureTrainInfo(planId, req));
  }

  @PostMapping("/roster/parse")
  @PreAuthorize(TrainingAuthorityExpressions.DIVISION_MANAGE)
  @AuditLog(module = "training", action = "division:roster-parse", targetType = "train_plan_roster")
  public ApiResponse<Map<String, Object>> parseRoster(@Valid @RequestBody TrainDivisionRosterParseReq req) {
    return ApiResponse.ok(divisionService.parseRoster(req));
  }

  @GetMapping("/roster")
  @PreAuthorize(TrainingAuthorityExpressions.DIVISION_VIEW)
  public ApiResponse<PageResponse<TrainPlanRosterVO>> pageRoster(
      @RequestParam Long planId,
      @RequestParam(required = false) Long trainInfoId,
      @RequestParam(required = false) Integer assignStatus,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "50") int pageSize) {
    return ApiResponse.ok(divisionService.pageRoster(planId, trainInfoId, assignStatus, page, pageSize));
  }

  @PostMapping("/auto-pack")
  @PreAuthorize(TrainingAuthorityExpressions.DIVISION_MANAGE)
  @AuditLog(module = "training", action = "division:auto-pack", targetType = "train_division_pack_task")
  public ApiResponse<Map<String, Object>> autoPack(@Valid @RequestBody TrainDivisionAutoPackReq req) {
    return ApiResponse.ok(divisionService.autoPack(req));
  }

  @GetMapping("/pack-tasks")
  @PreAuthorize(TrainingAuthorityExpressions.DIVISION_VIEW)
  public ApiResponse<PageResponse<Map<String, Object>>> pagePackTasks(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int pageSize,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long planId) {
    return ApiResponse.ok(divisionService.pagePackTasks(page, pageSize, status, planId));
  }

  @GetMapping("/pack-tasks/{taskId}")
  @PreAuthorize(TrainingAuthorityExpressions.DIVISION_VIEW)
  public ApiResponse<Map<String, Object>> getPackTask(@PathVariable Long taskId) {
    return ApiResponse.ok(divisionService.getPackTask(taskId));
  }

  @PostMapping("/pack-tasks/{taskId}/patch")
  @PreAuthorize(TrainingAuthorityExpressions.DIVISION_MANAGE)
  @AuditLog(module = "training", action = "division:pack-patch", targetType = "train_division_pack_task")
  public ApiResponse<Map<String, Object>> patchPackTask(
      @PathVariable Long taskId,
      @Valid @RequestBody TrainDivisionPackPatchReq req) {
    return ApiResponse.ok(divisionService.patchPackTask(taskId, req));
  }

  @PostMapping("/pack-tasks/{taskId}/submit")
  @PreAuthorize(TrainingAuthorityExpressions.DIVISION_MANAGE)
  @AuditLog(module = "training", action = "division:pack-submit", targetType = "train_division_pack_task")
  public ApiResponse<Map<String, Object>> submitPackTask(@PathVariable Long taskId) {
    return ApiResponse.ok(divisionService.submitPackTask(taskId));
  }

  @PostMapping("/pack-tasks/{taskId}/company-confirm")
  @PreAuthorize(TrainingAuthorityExpressions.INFO_CLASS_ENTERPRISE_CONFIRM)
  @AuditLog(module = "training", action = "division:pack-confirm", targetType = "train_division_pack_task")
  public ApiResponse<Map<String, Object>> companyConfirmPackTask(@PathVariable Long taskId) {
    return ApiResponse.ok(divisionService.companyConfirmPackTask(taskId));
  }

  @PostMapping("/pack-tasks/{taskId}/company-reject")
  @PreAuthorize(TrainingAuthorityExpressions.INFO_CLASS_ENTERPRISE_CONFIRM)
  @AuditLog(module = "training", action = "division:pack-reject", targetType = "train_division_pack_task")
  public ApiResponse<Map<String, Object>> companyRejectPackTask(
      @PathVariable Long taskId,
      @RequestParam String reason) {
    return ApiResponse.ok(divisionService.companyRejectPackTask(taskId, reason));
  }

  @PostMapping("/org-split")
  @PreAuthorize(TrainingAuthorityExpressions.DIVISION_MANAGE)
  @AuditLog(module = "training", action = "division:org-split", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> orgSplit(@Valid @RequestBody TrainDivisionOrgSplitReq req) {
    return ApiResponse.ok(divisionService.orgSplit(req));
  }

  @GetMapping("/train-info/{id}/summary")
  @PreAuthorize(TrainingAuthorityExpressions.DIVISION_VIEW)
  public ApiResponse<Map<String, Object>> trainInfoSummary(@PathVariable Long id) {
    return ApiResponse.ok(divisionService.trainInfoSummary(id));
  }
}
