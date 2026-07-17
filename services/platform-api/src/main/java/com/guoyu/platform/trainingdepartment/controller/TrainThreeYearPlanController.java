package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainThreeYearPlanCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainThreeYearPlanItemQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainThreeYearPlanItemUpdateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainThreeYearPlanQueryReq;
import com.guoyu.platform.trainingdepartment.security.TrainingAuthorityExpressions;
import com.guoyu.platform.trainingdepartment.service.TrainThreeYearPlanApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainThreeYearPlanAnalysisStatusVO;
import com.guoyu.platform.trainingdepartment.vo.TrainThreeYearPlanDetailVO;
import com.guoyu.platform.trainingdepartment.vo.TrainThreeYearPlanDistributionVO;
import com.guoyu.platform.trainingdepartment.vo.TrainThreeYearPlanItemVO;
import com.guoyu.platform.trainingdepartment.vo.TrainThreeYearPlanMatrixVO;
import com.guoyu.platform.trainingdepartment.vo.TrainThreeYearPlanVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/training")
public class TrainThreeYearPlanController {

  private final TrainThreeYearPlanApplicationService trainThreeYearPlanApplicationService;

  public TrainThreeYearPlanController(TrainThreeYearPlanApplicationService trainThreeYearPlanApplicationService) {
    this.trainThreeYearPlanApplicationService = trainThreeYearPlanApplicationService;
  }

  @GetMapping("/three-year-plans")
  @PreAuthorize(TrainingAuthorityExpressions.THREE_YEAR_PLAN_VIEW)
  public ApiResponse<PageResponse<TrainThreeYearPlanVO>> page(@Valid @ModelAttribute TrainThreeYearPlanQueryReq req) {
    return ApiResponse.ok(trainThreeYearPlanApplicationService.page(req));
  }

  @GetMapping("/three-year-plans/{id:\\d+}")
  @PreAuthorize(TrainingAuthorityExpressions.THREE_YEAR_PLAN_VIEW)
  public ApiResponse<TrainThreeYearPlanVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainThreeYearPlanApplicationService.getById(id));
  }

  @GetMapping("/three-year-plans/{id:\\d+}/detail-bundle")
  @PreAuthorize(TrainingAuthorityExpressions.THREE_YEAR_PLAN_VIEW)
  public ApiResponse<TrainThreeYearPlanDetailVO> getDetailBundle(
      @PathVariable Long id,
      @Valid @ModelAttribute TrainThreeYearPlanItemQueryReq req) {
    return ApiResponse.ok(trainThreeYearPlanApplicationService.getDetailBundle(id, req));
  }

  @PostMapping(value = "/three-year-plans", consumes = "multipart/form-data")
  @PreAuthorize("hasAuthority('pxanfangguihua:manage')")
  @AuditLog(module = "training", action = "three-year-plan:create", targetType = "train_three_year_plan")
  public ApiResponse<TrainThreeYearPlanVO> create(
      @RequestParam String title,
      @RequestParam Long targetOrgId,
      @RequestParam(required = false) Long researchOrgId,
      @RequestPart("file") MultipartFile file) {
    TrainThreeYearPlanCreateReq req = new TrainThreeYearPlanCreateReq(title, targetOrgId, researchOrgId);
    return ApiResponse.ok(trainThreeYearPlanApplicationService.createWithFile(req, file));
  }

  @GetMapping("/three-year-plans/{id:\\d+}/analysis-status")
  @PreAuthorize(TrainingAuthorityExpressions.THREE_YEAR_PLAN_VIEW)
  public ApiResponse<TrainThreeYearPlanAnalysisStatusVO> analysisStatus(@PathVariable Long id) {
    return ApiResponse.ok(trainThreeYearPlanApplicationService.refreshAnalysisStatus(id));
  }

  @PostMapping("/three-year-plans/{id:\\d+}/sync-from-rencai")
  @PreAuthorize("hasAuthority('pxanfangguihua:manage')")
  @AuditLog(module = "training", action = "three-year-plan:sync", targetType = "train_three_year_plan")
  public ApiResponse<TrainThreeYearPlanVO> syncFromRencai(@PathVariable Long id) {
    return ApiResponse.ok(trainThreeYearPlanApplicationService.syncFromRencai(id));
  }

  @PostMapping("/three-year-plans/{id:\\d+}/prepare-review")
  @PreAuthorize("hasAuthority('pxanfangguihua:manage')")
  public ApiResponse<TrainThreeYearPlanVO> prepareReview(@PathVariable Long id) {
    return ApiResponse.ok(trainThreeYearPlanApplicationService.prepareReview(id));
  }

  @PostMapping(value = "/three-year-plans/{id:\\d+}/submit", consumes = "multipart/form-data")
  @PreAuthorize("hasAuthority('pxanfangguihua:manage')")
  @AuditLog(module = "training", action = "three-year-plan:submit", targetType = "train_three_year_plan")
  public ApiResponse<TrainThreeYearPlanVO> submit(
      @PathVariable Long id,
      @RequestPart("personnelFile") MultipartFile personnelFile,
      @RequestPart("schemeFile") MultipartFile schemeFile) {
    return ApiResponse.ok(trainThreeYearPlanApplicationService.submit(id, personnelFile, schemeFile));
  }

  @PostMapping("/three-year-plans/{id:\\d+}/company-confirm")
  @PreAuthorize(TrainingAuthorityExpressions.THREE_YEAR_PLAN_ENTERPRISE_CONFIRM)
  @AuditLog(module = "training", action = "three-year-plan:company-confirm", targetType = "train_three_year_plan")
  public ApiResponse<TrainThreeYearPlanVO> companyConfirm(@PathVariable Long id) {
    return ApiResponse.ok(trainThreeYearPlanApplicationService.companyConfirm(id));
  }

  @PostMapping(value = "/three-year-plans/{id:\\d+}/enterprise-return", consumes = "multipart/form-data")
  @PreAuthorize(TrainingAuthorityExpressions.THREE_YEAR_PLAN_ENTERPRISE_CONFIRM)
  @AuditLog(module = "training", action = "three-year-plan:enterprise-return", targetType = "train_three_year_plan")
  public ApiResponse<TrainThreeYearPlanVO> uploadEnterpriseReturn(
      @PathVariable Long id,
      @RequestPart(value = "rosterFile", required = false) MultipartFile rosterFile,
      @RequestPart(value = "shiftFile", required = false) MultipartFile shiftFile) {
    return ApiResponse.ok(trainThreeYearPlanApplicationService.uploadEnterpriseReturn(id, rosterFile, shiftFile));
  }

  @PostMapping("/three-year-plans/{id:\\d+}/company-reject")
  @PreAuthorize(TrainingAuthorityExpressions.THREE_YEAR_PLAN_ENTERPRISE_CONFIRM)
  @AuditLog(module = "training", action = "three-year-plan:company-reject", targetType = "train_three_year_plan")
  public ApiResponse<TrainThreeYearPlanVO> companyReject(@PathVariable Long id, @RequestParam String reason) {
    return ApiResponse.ok(trainThreeYearPlanApplicationService.companyReject(id, reason));
  }

  @GetMapping("/three-year-plans/{id:\\d+}/items")
  @PreAuthorize(TrainingAuthorityExpressions.THREE_YEAR_PLAN_VIEW)
  public ApiResponse<PageResponse<TrainThreeYearPlanItemVO>> pageItems(
      @PathVariable Long id,
      @Valid @ModelAttribute TrainThreeYearPlanItemQueryReq req) {
    return ApiResponse.ok(trainThreeYearPlanApplicationService.pageItems(id, req));
  }

  @PutMapping("/three-year-plans/{planId:\\d+}/items/{itemId:\\d+}")
  @PreAuthorize("hasAuthority('pxanfangguihua:manage')")
  @AuditLog(module = "training", action = "three-year-plan:update-item", targetType = "train_three_year_plan_item")
  public ApiResponse<TrainThreeYearPlanItemVO> updateItem(
      @PathVariable Long planId,
      @PathVariable Long itemId,
      @RequestBody TrainThreeYearPlanItemUpdateReq req) {
    return ApiResponse.ok(trainThreeYearPlanApplicationService.updateItem(planId, itemId, req));
  }

  @GetMapping("/three-year-plans/{id:\\d+}/matrix")
  @PreAuthorize(TrainingAuthorityExpressions.THREE_YEAR_PLAN_VIEW)
  public ApiResponse<List<TrainThreeYearPlanMatrixVO>> listMatrix(@PathVariable Long id) {
    return ApiResponse.ok(trainThreeYearPlanApplicationService.listMatrix(id));
  }

  @GetMapping("/three-year-plans/{id:\\d+}/review-distribution")
  @PreAuthorize(TrainingAuthorityExpressions.THREE_YEAR_PLAN_VIEW)
  public ApiResponse<List<TrainThreeYearPlanDistributionVO>> reviewDistribution(@PathVariable Long id) {
    return ApiResponse.ok(trainThreeYearPlanApplicationService.getReviewDistribution(id));
  }

  @GetMapping("/three-year-plans/{id:\\d+}/download-plan")
  @PreAuthorize(TrainingAuthorityExpressions.THREE_YEAR_PLAN_VIEW)
  public ResponseEntity<byte[]> downloadPlan(@PathVariable Long id) {
    return trainThreeYearPlanApplicationService.downloadPlanFile(id);
  }

  @GetMapping("/three-year-plans/{id:\\d+}/download-reviewed-plan")
  @PreAuthorize(TrainingAuthorityExpressions.THREE_YEAR_PLAN_VIEW)
  public ResponseEntity<byte[]> downloadReviewedPlan(@PathVariable Long id) {
    return trainThreeYearPlanApplicationService.downloadReviewedPlanFile(id);
  }

  @GetMapping("/three-year-plans/{id:\\d+}/download-submit-personnel")
  @PreAuthorize(TrainingAuthorityExpressions.THREE_YEAR_PLAN_VIEW)
  public ResponseEntity<byte[]> downloadSubmitPersonnel(@PathVariable Long id) {
    return trainThreeYearPlanApplicationService.downloadSubmitPersonnelFile(id);
  }

  @GetMapping("/three-year-plans/{id:\\d+}/download-submit-scheme")
  @PreAuthorize(TrainingAuthorityExpressions.THREE_YEAR_PLAN_VIEW)
  public ResponseEntity<byte[]> downloadSubmitScheme(@PathVariable Long id) {
    return trainThreeYearPlanApplicationService.downloadSubmitSchemeFile(id);
  }

  @GetMapping("/three-year-plans/{id:\\d+}/download-enterprise-roster")
  @PreAuthorize(TrainingAuthorityExpressions.THREE_YEAR_PLAN_VIEW)
  public ResponseEntity<byte[]> downloadEnterpriseRoster(@PathVariable Long id) {
    return trainThreeYearPlanApplicationService.downloadEnterpriseRosterFile(id);
  }

  @GetMapping("/three-year-plans/{id:\\d+}/download-enterprise-shift")
  @PreAuthorize(TrainingAuthorityExpressions.THREE_YEAR_PLAN_VIEW)
  public ResponseEntity<byte[]> downloadEnterpriseShift(@PathVariable Long id) {
    return trainThreeYearPlanApplicationService.downloadEnterpriseShiftFile(id);
  }

  @GetMapping("/three-year-plans/{id:\\d+}/download-result")
  @PreAuthorize(TrainingAuthorityExpressions.THREE_YEAR_PLAN_VIEW)
  public ResponseEntity<byte[]> downloadResult(@PathVariable Long id) {
    return trainThreeYearPlanApplicationService.downloadResultZip(id);
  }

  @DeleteMapping("/three-year-plans/{id:\\d+}")
  @PreAuthorize("hasAuthority('pxanfangguihua:manage')")
  @AuditLog(module = "training", action = "three-year-plan:delete", targetType = "train_three_year_plan")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainThreeYearPlanApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
