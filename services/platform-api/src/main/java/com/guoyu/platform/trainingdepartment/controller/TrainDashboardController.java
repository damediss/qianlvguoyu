package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.trainingdepartment.service.TrainDashboardService;
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

  private final TrainDashboardService service;

  public TrainDashboardController(TrainDashboardService service) {
    this.service = service;
  }

  /** 获取看板数据 */
  @GetMapping("/dashboard")
  @PreAuthorize("hasAuthority('train:dashboard:view')")
  public ApiResponse<TrainDashboardVO> getDashboard() {
    return ApiResponse.ok(service.getDashboard());
  }

  /** 获取里程碑 */
  @GetMapping("/dashboard/milestones")
  @PreAuthorize("hasAuthority('train:dashboard:view')")
  public ApiResponse<List<TrainDashboardVO.MilestoneItem>> getMilestones() {
    return ApiResponse.ok(service.getMilestones());
  }

  @GetMapping("/dashboard/stage-distribution")
  @PreAuthorize("hasAuthority('train:dashboard:view')")
  public ApiResponse<Map<String, Long>> getStageDistribution() {
    return ApiResponse.ok(service.getStageDistribution());
  }
}
