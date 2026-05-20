package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainNeedCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainNeedQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainNeedUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainNeedService;
import com.guoyu.platform.trainingdepartment.vo.TrainNeedVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 培训需求管理接口
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainNeedController {

  private final TrainNeedService service;

  public TrainNeedController(TrainNeedService service) {
    this.service = service;
  }

  /** 分页查询 */
  @GetMapping("/needs")
  @PreAuthorize("hasAuthority('train:need:view')")
  public ApiResponse<PageResponse<TrainNeedVO>> page(@Valid @ModelAttribute TrainNeedQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  /** 根据ID查询 */
  @GetMapping("/needs/{id}")
  @PreAuthorize("hasAuthority('train:need:view')")
  public ApiResponse<TrainNeedVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  /** 创建 */
  @PostMapping("/needs")
  @PreAuthorize("hasAuthority('train:need:create')")
  @AuditLog(module = "training", action = "need:create", targetType = "train_need")
  public ApiResponse<TrainNeedVO> create(@Valid @RequestBody TrainNeedCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  /** 更新 */
  @PutMapping("/needs/{id}")
  @PreAuthorize("hasAuthority('train:need:update')")
  @AuditLog(module = "training", action = "need:update", targetType = "train_need")
  public ApiResponse<TrainNeedVO> update(@PathVariable Long id, @Valid @RequestBody TrainNeedUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  /** 删除 */
  @DeleteMapping("/needs/{id}")
  @PreAuthorize("hasAuthority('train:need:delete')")
  @AuditLog(module = "training", action = "need:delete", targetType = "train_need")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }

  @PostMapping("/needs/import")
  @PreAuthorize("hasAuthority('train:need:create')")
  @AuditLog(module = "training", action = "need:import", targetType = "train_need")
  public ApiResponse<Map<String, Object>> importNeeds(@RequestParam("file") MultipartFile file) {
    // TODO: implement with EasyExcel in future iteration
    Map<String, Object> result = new HashMap<>();
    result.put("total", 0);
    result.put("success", 0);
    result.put("message", "导入功能开发中");
    return ApiResponse.ok(result);
  }
}
