package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainProjectTableCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainProjectTableQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainProjectTableUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainProjectTableService;
import com.guoyu.platform.trainingdepartment.vo.TrainProjectTableVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 培训项目表单管理接口
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainProjectTableController {

  private final TrainProjectTableService service;

  public TrainProjectTableController(TrainProjectTableService service) {
    this.service = service;
  }

  /** 分页查询 */
  @GetMapping("/project-tables")
  @PreAuthorize("hasAuthority('train:project-table:view')")
  public ApiResponse<PageResponse<TrainProjectTableVO>> page(@Valid @ModelAttribute TrainProjectTableQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  /** 根据ID查询 */
  @GetMapping("/project-tables/{id}")
  @PreAuthorize("hasAuthority('train:project-table:view')")
  public ApiResponse<TrainProjectTableVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  /** 创建 */
  @PostMapping("/project-tables")
  @PreAuthorize("hasAuthority('train:project-table:create')")
  @AuditLog(module = "training", action = "project-table:create", targetType = "train_project_table")
  public ApiResponse<TrainProjectTableVO> create(@Valid @RequestBody TrainProjectTableCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  /** 更新 */
  @PutMapping("/project-tables/{id}")
  @PreAuthorize("hasAuthority('train:project-table:update')")
  @AuditLog(module = "training", action = "project-table:update", targetType = "train_project_table")
  public ApiResponse<TrainProjectTableVO> update(@PathVariable Long id, @Valid @RequestBody TrainProjectTableUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  /** 删除 */
  @DeleteMapping("/project-tables/{id}")
  @PreAuthorize("hasAuthority('train:project-table:delete')")
  @AuditLog(module = "training", action = "project-table:delete", targetType = "train_project_table")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
