package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.trainingdepartment.service.TrainDashboardApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainDashboardVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 培训数据看板管理接口
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainDashboardController {

  private final TrainDashboardApplicationService trainDashboardApplicationService;

  public TrainDashboardController(TrainDashboardApplicationService trainDashboardApplicationService) {
    this.trainDashboardApplicationService = trainDashboardApplicationService;
  }

  /** 获取看板数据 */
  @GetMapping("/dashboard")
  @PreAuthorize("hasAuthority('train:dashboard:view')")
  public ApiResponse<TrainDashboardVO> getDashboard() {
    return ApiResponse.ok(trainDashboardApplicationService.getDashboard());
  }

  /** 获取里程碑 */
  @GetMapping("/dashboard/milestones")
  @PreAuthorize("hasAuthority('train:dashboard:view')")
  public ApiResponse<List<TrainDashboardVO.MilestoneItem>> getMilestones() {
    return ApiResponse.ok(trainDashboardApplicationService.getMilestones());
  }

  @GetMapping("/dashboard/stage-distribution")
  @PreAuthorize("hasAuthority('train:dashboard:view')")
  public ApiResponse<Map<String, Long>> getStageDistribution() {
    return ApiResponse.ok(trainDashboardApplicationService.getStageDistribution());
  }

  /** 企业培训明细 */
  @GetMapping("/dashboard/companies")
  @PreAuthorize("hasAuthority('train:dashboard:view')")
  public ApiResponse<List<TrainDashboardVO.CompanyOverview>> getCompanies() {
    return ApiResponse.ok(trainDashboardApplicationService.getCompanyOverviews());
  }
}
