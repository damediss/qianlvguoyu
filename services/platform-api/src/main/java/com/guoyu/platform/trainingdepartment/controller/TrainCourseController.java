package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainCourseCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainCourseQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainCourseUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainCourseService;
import com.guoyu.platform.trainingdepartment.vo.TrainCourseVO;
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
 * 课时列表管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainCourseController {

  private final TrainCourseService service;

  public TrainCourseController(TrainCourseService service) {
    this.service = service;
  }

  /** 分页查询课时列表 */
  @GetMapping("/courses")
  @PreAuthorize("hasAuthority('train:course:view')")
  public ApiResponse<PageResponse<TrainCourseVO>> page(@Valid @ModelAttribute TrainCourseQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  /** 根据ID查询课时详情 */
  @GetMapping("/courses/{id}")
  @PreAuthorize("hasAuthority('train:course:view')")
  public ApiResponse<TrainCourseVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  /** 新增课时 */
  @PostMapping("/courses")
  @PreAuthorize("hasAuthority('train:course:create')")
  @AuditLog(module = "training", action = "course:create", targetType = "train_course")
  public ApiResponse<TrainCourseVO> create(@Valid @RequestBody TrainCourseCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  /** 修改课时 */
  @PutMapping("/courses/{id}")
  @PreAuthorize("hasAuthority('train:course:update')")
  @AuditLog(module = "training", action = "course:update", targetType = "train_course")
  public ApiResponse<TrainCourseVO> update(@PathVariable Long id, @Valid @RequestBody TrainCourseUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  /** 删除课时 */
  @DeleteMapping("/courses/{id}")
  @PreAuthorize("hasAuthority('train:course:delete')")
  @AuditLog(module = "training", action = "course:delete", targetType = "train_course")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
