package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.trainingdepartment.service.ReviewSyncPanelApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainDashboardApplicationService;
import com.guoyu.platform.trainingdepartment.vo.ReviewSyncPanelVO;
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
  private final ReviewSyncPanelApplicationService reviewSyncPanelApplicationService;

  public TrainDashboardController(TrainDashboardApplicationService trainDashboardApplicationService,
      ReviewSyncPanelApplicationService reviewSyncPanelApplicationService) {
    this.trainDashboardApplicationService = trainDashboardApplicationService;
    this.reviewSyncPanelApplicationService = reviewSyncPanelApplicationService;
  }

  /** 获取看板数据 */
  @GetMapping("/dashboard")
  @PreAuthorize("hasAuthority('train:dashboard:view')")
  public ApiResponse<TrainDashboardVO> getDashboard() {
    return ApiResponse.ok(trainDashboardApplicationService.getDashboard());
  }

  /** 同步练习（AI 同步学）统计面板：一次拿全，默认近 30 天口径 */
  @GetMapping("/dashboard/review-sync")
  @PreAuthorize("hasAuthority('train:dashboard:view')")
  public ApiResponse<ReviewSyncPanelVO> getReviewSyncPanel(
      @RequestParam(required = false) Integer days) {
    return ApiResponse.ok(reviewSyncPanelApplicationService.getPanel(days));
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
