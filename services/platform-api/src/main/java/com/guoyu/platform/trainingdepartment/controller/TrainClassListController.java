package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.AssignClassReq;
import com.guoyu.platform.trainingdepartment.dto.TrainClassListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainClassListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainClassListUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainClassListService;
import com.guoyu.platform.trainingdepartment.vo.TrainClassListVO;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 分班管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainClassListController {

  private final TrainClassListService service;

  public TrainClassListController(TrainClassListService service) {
    this.service = service;
  }

  /** 分页查询分班列表 */
  @GetMapping("/class-lists")
  @PreAuthorize("hasAuthority('train:class-list:view')")
  public ApiResponse<PageResponse<TrainClassListVO>> page(@Valid @ModelAttribute TrainClassListQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  /** 根据ID查询分班详情 */
  @GetMapping("/class-lists/{id}")
  @PreAuthorize("hasAuthority('train:class-list:view')")
  public ApiResponse<TrainClassListVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  /** 新增分班 */
  @PostMapping("/class-lists")
  @PreAuthorize("hasAuthority('train:class-list:create')")
  @AuditLog(module = "training", action = "class-list:create", targetType = "train_class_list")
  public ApiResponse<TrainClassListVO> create(@Valid @RequestBody TrainClassListCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  /** 修改分班 */
  @PutMapping("/class-lists/{id}")
  @PreAuthorize("hasAuthority('train:class-list:update')")
  @AuditLog(module = "training", action = "class-list:update", targetType = "train_class_list")
  public ApiResponse<TrainClassListVO> update(@PathVariable Long id, @Valid @RequestBody TrainClassListUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  /** 删除分班 */
  @DeleteMapping("/class-lists/{id}")
  @PreAuthorize("hasAuthority('train:class-list:delete')")
  @AuditLog(module = "training", action = "class-list:delete", targetType = "train_class_list")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }

  /** 批量分配学员到班级 */
  @PostMapping("/class-lists/batch-assign")
  @PreAuthorize("hasAuthority('train:class-list:assign')")
  @AuditLog(module = "training", action = "class-list:batch-assign", targetType = "train_class_list")
  public ApiResponse<Void> batchAssign(@Valid @RequestBody AssignClassReq req) {
    service.batchAssign(req);
    return ApiResponse.ok();
  }

  /** 开班 */
  @PostMapping("/class-lists/{id}/open")
  @PreAuthorize("hasAuthority('train:class-list:update')")
  @AuditLog(module = "training", action = "class-list:open", targetType = "train_class_list")
  public ApiResponse<TrainClassListVO> openClass(@PathVariable Long id) {
    return ApiResponse.ok(service.openClass(id));
  }

  /** 结课 */
  @PostMapping("/class-lists/{id}/close")
  @PreAuthorize("hasAuthority('train:class-list:update')")
  @AuditLog(module = "training", action = "class-list:close", targetType = "train_class_list")
  public ApiResponse<TrainClassListVO> closeClass(@PathVariable Long id) {
    return ApiResponse.ok(service.closeClass(id));
  }
}
