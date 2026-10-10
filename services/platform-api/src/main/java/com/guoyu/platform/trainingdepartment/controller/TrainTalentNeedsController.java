package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainTalentNeedsCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTalentNeedsQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTalentNeedsUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainTalentNeedsApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainTalentNeedsVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 人才需求管理接口
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainTalentNeedsController {

  private final TrainTalentNeedsApplicationService trainTalentNeedsApplicationService;

  public TrainTalentNeedsController(TrainTalentNeedsApplicationService trainTalentNeedsApplicationService) {
    this.trainTalentNeedsApplicationService = trainTalentNeedsApplicationService;
  }

  /** 分页查询 */
  @GetMapping("/talent-needs")
  @PreAuthorize("hasAuthority('train:talent-needs:view')")
  public ApiResponse<PageResponse<TrainTalentNeedsVO>> page(@Valid @ModelAttribute TrainTalentNeedsQueryReq req) {
    return ApiResponse.ok(trainTalentNeedsApplicationService.page(req));
  }

  /** 根据ID查询 */
  @GetMapping("/talent-needs/{id}")
  @PreAuthorize("hasAuthority('train:talent-needs:view')")
  public ApiResponse<TrainTalentNeedsVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainTalentNeedsApplicationService.getById(id));
  }

  /** 创建 */
  @PostMapping("/talent-needs")
  @PreAuthorize("hasAuthority('train:talent-needs:create')")
  @AuditLog(module = "training", action = "talent-needs:create", targetType = "train_talent_needs")
  public ApiResponse<TrainTalentNeedsVO> create(@Valid @RequestBody TrainTalentNeedsCreateReq req) {
    return ApiResponse.ok(trainTalentNeedsApplicationService.create(req));
  }

  /** 更新 */
  @PutMapping("/talent-needs/{id}")
  @PreAuthorize("hasAuthority('train:talent-needs:update')")
  @AuditLog(module = "training", action = "talent-needs:update", targetType = "train_talent_needs")
  public ApiResponse<TrainTalentNeedsVO> update(@PathVariable Long id, @Valid @RequestBody TrainTalentNeedsUpdateReq req) {
    return ApiResponse.ok(trainTalentNeedsApplicationService.update(id, req));
  }

  /** 删除 */
  @DeleteMapping("/talent-needs/{id}")
  @PreAuthorize("hasAuthority('train:talent-needs:delete')")
  @AuditLog(module = "training", action = "talent-needs:delete", targetType = "train_talent_needs")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainTalentNeedsApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
