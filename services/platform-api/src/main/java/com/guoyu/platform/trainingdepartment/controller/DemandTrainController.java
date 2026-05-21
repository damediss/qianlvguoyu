package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.DemandConvertReq;
import com.guoyu.platform.trainingdepartment.dto.DemandTrainCreateReq;
import com.guoyu.platform.trainingdepartment.dto.DemandTrainQueryReq;
import com.guoyu.platform.trainingdepartment.dto.DemandTrainUpdateReq;
import com.guoyu.platform.trainingdepartment.service.DemandTrainApplicationService;
import com.guoyu.platform.trainingdepartment.vo.DemandPendingVO;
import com.guoyu.platform.trainingdepartment.vo.DemandTrainVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training")
public class DemandTrainController {

  private final DemandTrainApplicationService demandTrainApplicationService;

  public DemandTrainController(DemandTrainApplicationService demandTrainApplicationService) {
    this.demandTrainApplicationService = demandTrainApplicationService;
  }

  @GetMapping("/demand-trains")
  @PreAuthorize("hasAuthority('train:demand-train:view')")
  public ApiResponse<PageResponse<DemandTrainVO>> page(@Valid @ModelAttribute DemandTrainQueryReq req) {
    return ApiResponse.ok(demandTrainApplicationService.page(req));
  }

  @GetMapping("/demand-trains/{id:\\d+}")
  @PreAuthorize("hasAuthority('train:demand-train:view')")
  public ApiResponse<DemandTrainVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(demandTrainApplicationService.getById(id));
  }

  @PostMapping("/demand-trains")
  @PreAuthorize("hasAuthority('train:demand-train:create')")
  @AuditLog(module = "training", action = "demand-train:create", targetType = "demand-trains")
  public ApiResponse<DemandTrainVO> create(@Valid @RequestBody DemandTrainCreateReq req) {
    return ApiResponse.ok(demandTrainApplicationService.create(req));
  }

  @PutMapping("/demand-trains/{id:\\d+}")
  @PreAuthorize("hasAuthority('train:demand-train:update')")
  @AuditLog(module = "training", action = "demand-train:update", targetType = "demand-trains")
  public ApiResponse<DemandTrainVO> update(@PathVariable Long id, @Valid @RequestBody DemandTrainUpdateReq req) {
    return ApiResponse.ok(demandTrainApplicationService.update(id, req));
  }

  @DeleteMapping("/demand-trains/{id:\\d+}")
  @PreAuthorize("hasAuthority('train:demand-train:delete')")
  @AuditLog(module = "training", action = "demand-train:delete", targetType = "demand-trains")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    demandTrainApplicationService.delete(id);
    return ApiResponse.ok();
  }

  // ======== 需求管理业务接口 ========

  /** 待处理需求列表（status=0） */
  @GetMapping("/demand-trains/pendings")
  @PreAuthorize("hasAuthority('train:demand-train:view')")
  public ApiResponse<PageResponse<DemandPendingVO>> getPendings(
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "30") Integer pageSize,
      @RequestParam(required = false) String keyword) {
    return ApiResponse.ok(demandTrainApplicationService.getPendings(page, pageSize, keyword));
  }

  /** 公共数据（企业列表、工种列表、培训信息列表） */
  @GetMapping("/demand-trains/public-data")
  @PreAuthorize("hasAuthority('train:demand-train:view')")
  public ApiResponse<Map<String, Object>> getPublicData(@RequestParam(required = false) List<String> tags) {
    return ApiResponse.ok(demandTrainApplicationService.getPublicData(tags));
  }

  /** 需求转培训开班 */
  @PostMapping("/demand-trains/convert")
  @PreAuthorize("hasAuthority('train:demand-train:create')")
  @AuditLog(module = "training", action = "demand-train:convert", targetType = "train_info_list")
  public ApiResponse<Map<String, Object>> convertToTraining(@Valid @RequestBody DemandConvertReq req) {
    return ApiResponse.ok(demandTrainApplicationService.convertToTraining(req));
  }
}
