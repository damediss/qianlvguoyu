package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.system.vo.OrgSimpleVO;
import com.guoyu.platform.trainingdepartment.dto.EnsureSystemCompanyReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchOrgCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchOrgQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchOrgUpdateReq;
import com.guoyu.platform.trainingdepartment.security.TrainingAuthorityExpressions;
import com.guoyu.platform.trainingdepartment.service.TrainResearchOrgApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainResearchOrgVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 培训调研企业/机构管理
 */
@Validated
@RestController
@RequestMapping("/training/research-orgs")
public class TrainResearchOrgController {

  private final TrainResearchOrgApplicationService trainResearchOrgApplicationService;

  public TrainResearchOrgController(TrainResearchOrgApplicationService trainResearchOrgApplicationService) {
    this.trainResearchOrgApplicationService = trainResearchOrgApplicationService;
  }

  @GetMapping
  @PreAuthorize(TrainingAuthorityExpressions.RESEARCH_ORG_VIEW)
  public ApiResponse<PageResponse<TrainResearchOrgVO>> page(@Valid TrainResearchOrgQueryReq req) {
    return ApiResponse.ok(trainResearchOrgApplicationService.page(req));
  }

  /** 手动输入企业名：查找或创建系统企业（company），供关联下拉使用 */
  @PostMapping("/system-companies")
  @PreAuthorize(TrainingAuthorityExpressions.RESEARCH_ORG_MUTATE)
  public ApiResponse<OrgSimpleVO> ensureSystemCompany(@Valid @RequestBody EnsureSystemCompanyReq req) {
    return ApiResponse.ok(trainResearchOrgApplicationService.ensureSystemCompany(req));
  }

  @GetMapping("/{id:\\d+}")
  @PreAuthorize(TrainingAuthorityExpressions.RESEARCH_ORG_VIEW)
  public ApiResponse<TrainResearchOrgVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainResearchOrgApplicationService.getById(id));
  }

  @PostMapping
  @PreAuthorize(TrainingAuthorityExpressions.RESEARCH_ORG_CREATE)
  public ApiResponse<TrainResearchOrgVO> create(@Valid @RequestBody TrainResearchOrgCreateReq req) {
    return ApiResponse.ok(trainResearchOrgApplicationService.create(req));
  }

  @PutMapping("/{id:\\d+}")
  @PreAuthorize(TrainingAuthorityExpressions.RESEARCH_ORG_UPDATE)
  public ApiResponse<TrainResearchOrgVO> update(@PathVariable Long id,
      @Valid @RequestBody TrainResearchOrgUpdateReq req) {
    return ApiResponse.ok(trainResearchOrgApplicationService.update(id, req));
  }

  @DeleteMapping("/{id:\\d+}")
  @PreAuthorize(TrainingAuthorityExpressions.RESEARCH_ORG_DELETE)
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainResearchOrgApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
