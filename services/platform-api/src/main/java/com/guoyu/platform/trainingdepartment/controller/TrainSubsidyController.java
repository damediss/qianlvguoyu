package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.SubsidyTransitionReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSubsidyCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSubsidyQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSubsidyUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainSubsidyApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainSubsidyPackProfileApplicationService;
import com.guoyu.platform.trainingdepartment.vo.SubsidyEventVO;
import com.guoyu.platform.trainingdepartment.vo.SubsidyPackProfileVO;
import com.guoyu.platform.trainingdepartment.vo.TrainSubsidyVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/training")
public class TrainSubsidyController {

  private final TrainSubsidyApplicationService trainSubsidyApplicationService;
  private final TrainSubsidyPackProfileApplicationService profileService;

  public TrainSubsidyController(
      TrainSubsidyApplicationService trainSubsidyApplicationService,
      TrainSubsidyPackProfileApplicationService profileService) {
    this.trainSubsidyApplicationService = trainSubsidyApplicationService;
    this.profileService = profileService;
  }

  @GetMapping("/subsidies")
  @PreAuthorize("hasAuthority('train:subsidy:view')")
  public ApiResponse<PageResponse<TrainSubsidyVO>> page(@Valid @ModelAttribute TrainSubsidyQueryReq req) {
    return ApiResponse.ok(trainSubsidyApplicationService.page(req));
  }

  @GetMapping("/subsidies/{id}")
  @PreAuthorize("hasAuthority('train:subsidy:view')")
  public ApiResponse<TrainSubsidyVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainSubsidyApplicationService.getById(id));
  }

  @PostMapping("/subsidies")
  @PreAuthorize("hasAuthority('train:subsidy:create')")
  @AuditLog(module = "training", action = "subsidy:create", targetType = "train_subsidy")
  public ApiResponse<TrainSubsidyVO> create(@Valid @RequestBody TrainSubsidyCreateReq req) {
    return ApiResponse.ok(trainSubsidyApplicationService.create(req));
  }

  @PutMapping("/subsidies/{id}")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:update", targetType = "train_subsidy")
  public ApiResponse<TrainSubsidyVO> update(@PathVariable Long id, @Valid @RequestBody TrainSubsidyUpdateReq req) {
    return ApiResponse.ok(trainSubsidyApplicationService.update(id, req));
  }

  @DeleteMapping("/subsidies/{id}")
  @PreAuthorize("hasAuthority('train:subsidy:delete')")
  @AuditLog(module = "training", action = "subsidy:delete", targetType = "train_subsidy")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainSubsidyApplicationService.delete(id);
    return ApiResponse.ok();
  }

  @PostMapping("/subsidies/{id}/transition")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:transition", targetType = "train_subsidy")
  public ApiResponse<TrainSubsidyVO> transition(
      @PathVariable Long id, @Valid @RequestBody SubsidyTransitionReq body) {
    return ApiResponse.ok(trainSubsidyApplicationService.transition(id, body));
  }

  @GetMapping("/subsidies/{id}/events")
  @PreAuthorize("hasAuthority('train:subsidy:view')")
  public ApiResponse<List<SubsidyEventVO>> events(@PathVariable Long id) {
    return ApiResponse.ok(trainSubsidyApplicationService.events(id));
  }

  @GetMapping("/subsidies/{id}/slot-files")
  @PreAuthorize("hasAuthority('train:subsidy:view')")
  public ApiResponse<List<Map<String, Object>>> slotFiles(@PathVariable Long id) {
    return ApiResponse.ok(trainSubsidyApplicationService.listSlotFiles(id));
  }

  @PostMapping(value = "/subsidies/{id}/slot-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:upload-slot", targetType = "train_subsidy")
  public ApiResponse<Map<String, Object>> uploadSlot(
      @PathVariable Long id,
      @RequestParam String slotCode,
      @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(trainSubsidyApplicationService.uploadSlotFile(id, slotCode, file));
  }

  @PostMapping("/subsidies/{id}/materials/generate")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:generate", targetType = "train_subsidy")
  public ApiResponse<Map<String, Object>> generate(@PathVariable Long id) {
    return ApiResponse.ok(trainSubsidyApplicationService.generatePack(id));
  }

  @GetMapping("/subsidy-pack-profiles")
  @PreAuthorize("hasAuthority('train:subsidy:view')")
  public ApiResponse<List<SubsidyPackProfileVO>> profiles(
      @RequestParam(required = false) String regionCode,
      @RequestParam(required = false) String trainMode,
      @RequestParam(required = false) Integer policyYear,
      @RequestParam(required = false) String status) {
    return ApiResponse.ok(profileService.list(regionCode, trainMode, policyYear, status));
  }

  @GetMapping("/subsidy-pack-profiles/{id}")
  @PreAuthorize("hasAuthority('train:subsidy:view')")
  public ApiResponse<SubsidyPackProfileVO> profile(@PathVariable Long id) {
    return ApiResponse.ok(profileService.get(id));
  }

  @PostMapping("/subsidies/{id}/submit")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:submit", targetType = "train_subsidy")
  public ApiResponse<TrainSubsidyVO> submit(@PathVariable Long id) {
    return ApiResponse.ok(trainSubsidyApplicationService.submit(id));
  }

  @PostMapping("/subsidies/{id}/approve")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:approve", targetType = "train_subsidy")
  public ApiResponse<TrainSubsidyVO> approve(@PathVariable Long id) {
    return ApiResponse.ok(trainSubsidyApplicationService.approve(id));
  }

  @PostMapping("/subsidies/{id}/reject")
  @PreAuthorize("hasAuthority('train:subsidy:update')")
  @AuditLog(module = "training", action = "subsidy:reject", targetType = "train_subsidy")
  public ApiResponse<TrainSubsidyVO> reject(@PathVariable Long id, @RequestParam String reason) {
    return ApiResponse.ok(trainSubsidyApplicationService.reject(id, reason));
  }
}
