package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionSaveReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainSolutionApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainSolutionBadgeVO;
import com.guoyu.platform.trainingdepartment.vo.TrainSolutionListVO;
import com.guoyu.platform.trainingdepartment.vo.TrainSolutionVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 培训方案管理接口
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainSolutionController {

  private final TrainSolutionApplicationService trainSolutionApplicationService;

  public TrainSolutionController(TrainSolutionApplicationService trainSolutionApplicationService) {
    this.trainSolutionApplicationService = trainSolutionApplicationService;
  }

  /** 分页查询 */
  @GetMapping("/solutions")
  @PreAuthorize("hasAuthority('train:solution:view')")
  public ApiResponse<PageResponse<TrainSolutionVO>> page(@Valid @ModelAttribute TrainSolutionQueryReq req) {
    return ApiResponse.ok(trainSolutionApplicationService.page(req));
  }

  /** 方案列表（含费用汇总和培训需求标题） */
  @GetMapping("/solutions/list")
  @PreAuthorize("hasAuthority('train:solution:view')")
  public ApiResponse<PageResponse<TrainSolutionListVO>> listForPage(
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer pageSize) {
    return ApiResponse.ok(trainSolutionApplicationService.listForPage(page, pageSize));
  }

  /** 根据ID查询 */
  @GetMapping("/solutions/{id:\\d+}")
  @PreAuthorize("hasAuthority('train:solution:view')")
  public ApiResponse<TrainSolutionVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainSolutionApplicationService.getById(id));
  }

  /** 方案详情（含经费明细和培训需求标题） */
  @GetMapping("/solutions/{id:\\d+}/detail")
  @PreAuthorize("hasAuthority('train:solution:view')")
  public ApiResponse<Map<String, Object>> getDetail(@PathVariable Long id) {
    return ApiResponse.ok(trainSolutionApplicationService.getDetail(id));
  }

  /** 创建 */
  @PostMapping("/solutions")
  @PreAuthorize("hasAuthority('train:solution:create')")
  @AuditLog(module = "training", action = "solution:create", targetType = "train_solution")
  public ApiResponse<PageResponse<TrainSolutionListVO>> create(@Valid @RequestBody TrainSolutionCreateReq req) {
    return ApiResponse.ok(trainSolutionApplicationService.create(req));
  }

  /** 保存（创建/更新 + 经费明细） */
  @PostMapping("/solutions/save")
  @PreAuthorize("hasAuthority('train:solution:create')")
  @AuditLog(module = "training", action = "solution:save", targetType = "train_solution")
  public ApiResponse<Map<String, Object>> save(@Valid @RequestBody TrainSolutionSaveReq req) {
    return ApiResponse.ok(trainSolutionApplicationService.save(req));
  }

  /** 更新 */
  @PutMapping("/solutions/{id:\\d+}")
  @PreAuthorize("hasAuthority('train:solution:update')")
  @AuditLog(module = "training", action = "solution:update", targetType = "train_solution")
  public ApiResponse<TrainSolutionVO> update(@PathVariable Long id, @Valid @RequestBody TrainSolutionUpdateReq req) {
    return ApiResponse.ok(trainSolutionApplicationService.update(id, req));
  }

  /** 删除 */
  @DeleteMapping("/solutions/{id:\\d+}")
  @PreAuthorize("hasAuthority('train:solution:delete')")
  @AuditLog(module = "training", action = "solution:delete", targetType = "train_solution")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainSolutionApplicationService.delete(id);
    return ApiResponse.ok();
  }

  /** 提交审批 */
  @PostMapping("/solutions/{id:\\d+}/submit")
  @PreAuthorize("hasAuthority('train:solution:update')")
  @AuditLog(module = "training", action = "solution:submit", targetType = "train_solution")
  public ApiResponse<TrainSolutionVO> submit(@PathVariable Long id) {
    return ApiResponse.ok(trainSolutionApplicationService.submit(id));
  }

  /** 审批通过 */
  @PostMapping("/solutions/{id:\\d+}/approve")
  @PreAuthorize("hasAuthority('train:solution:update')")
  @AuditLog(module = "training", action = "solution:approve", targetType = "train_solution")
  public ApiResponse<TrainSolutionVO> approve(@PathVariable Long id) {
    return ApiResponse.ok(trainSolutionApplicationService.approve(id));
  }

  /** 驳回 */
  @PostMapping("/solutions/{id:\\d+}/reject")
  @PreAuthorize("hasAuthority('train:solution:update')")
  @AuditLog(module = "training", action = "solution:reject", targetType = "train_solution")
  public ApiResponse<TrainSolutionVO> reject(@PathVariable Long id, @RequestParam String reason) {
    return ApiResponse.ok(trainSolutionApplicationService.reject(id, reason));
  }

  /** 根据需求ID获取方案详情 */
  @GetMapping("/solutions/by-demand-id")
  @PreAuthorize("hasAuthority('train:solution:view')")
  public ApiResponse<Map<String, Object>> getByDemandId(@RequestParam Long traindemandId) {
    return ApiResponse.ok(trainSolutionApplicationService.getByDemandId(traindemandId));
  }

  /** 查询经费明细 */
  @GetMapping("/solutions/{id:\\d+}/badges")
  @PreAuthorize("hasAuthority('train:solution:view')")
  public ApiResponse<List<TrainSolutionBadgeVO>> getBadges(@PathVariable Long id) {
    return ApiResponse.ok(trainSolutionApplicationService.getBadges(id));
  }

  /** 保存经费明细（全量替换） */
  @PutMapping("/solutions/{id:\\d+}/badges")
  @PreAuthorize("hasAuthority('train:solution:update')")
  @AuditLog(module = "training", action = "solution:badge:save", targetType = "train_solution_badge")
  public ApiResponse<Void> saveBadges(@PathVariable Long id, @RequestBody List<TrainSolutionBadgeVO> badges) {
    trainSolutionApplicationService.saveBadges(id, badges);
    return ApiResponse.ok();
  }
}
