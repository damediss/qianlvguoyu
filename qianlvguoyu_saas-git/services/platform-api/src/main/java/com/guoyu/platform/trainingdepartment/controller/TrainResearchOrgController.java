package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchOrgCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchOrgQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchOrgUpdateReq;
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
  @PreAuthorize("hasAuthority('train:research_org:view')")
  public ApiResponse<PageResponse<TrainResearchOrgVO>> page(@Valid TrainResearchOrgQueryReq req) {
    return ApiResponse.ok(trainResearchOrgApplicationService.page(req));
  }

  @GetMapping("/{id:\\d+}")
  @PreAuthorize("hasAuthority('train:research_org:view')")
  public ApiResponse<TrainResearchOrgVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainResearchOrgApplicationService.getById(id));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('train:research_org:create')")
  public ApiResponse<TrainResearchOrgVO> create(@Valid @RequestBody TrainResearchOrgCreateReq req) {
    return ApiResponse.ok(trainResearchOrgApplicationService.create(req));
  }

  @PutMapping("/{id:\\d+}")
  @PreAuthorize("hasAuthority('train:research_org:update')")
  public ApiResponse<TrainResearchOrgVO> update(@PathVariable Long id,
      @Valid @RequestBody TrainResearchOrgUpdateReq req) {
    return ApiResponse.ok(trainResearchOrgApplicationService.update(id, req));
  }

  @DeleteMapping("/{id:\\d+}")
  @PreAuthorize("hasAuthority('train:research_org:delete')")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainResearchOrgApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
