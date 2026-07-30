package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.CourseKejianCreateReq;
import com.guoyu.platform.trainingdepartment.dto.CourseKejianQueryReq;
import com.guoyu.platform.trainingdepartment.dto.CourseKejianUpdateReq;
import com.guoyu.platform.trainingdepartment.service.CourseKejianApplicationService;
import com.guoyu.platform.trainingdepartment.vo.CourseKejianVO;
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
 * 课程课件管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class CourseKejianController {

  private final CourseKejianApplicationService courseKejianApplicationService;

  public CourseKejianController(CourseKejianApplicationService courseKejianApplicationService) {
    this.courseKejianApplicationService = courseKejianApplicationService;
  }

  /** 分页查询课程课件列表 */
  @GetMapping("/course-kejians")
  @PreAuthorize("hasAuthority('train:course-kejian:view')")
  public ApiResponse<PageResponse<CourseKejianVO>> page(@Valid @ModelAttribute CourseKejianQueryReq req) {
    return ApiResponse.ok(courseKejianApplicationService.page(req));
  }

  /** 根据ID查询课程课件详情 */
  @GetMapping("/course-kejians/{id}")
  @PreAuthorize("hasAuthority('train:course-kejian:view')")
  public ApiResponse<CourseKejianVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(courseKejianApplicationService.getById(id));
  }

  /** 新增课程课件 */
  @PostMapping("/course-kejians")
  @PreAuthorize("hasAuthority('train:course-kejian:create')")
  @AuditLog(module = "training", action = "course-kejian:create", targetType = "course_kejian")
  public ApiResponse<CourseKejianVO> create(@Valid @RequestBody CourseKejianCreateReq req) {
    return ApiResponse.ok(courseKejianApplicationService.create(req));
  }

  /** 修改课程课件 */
  @PutMapping("/course-kejians/{id}")
  @PreAuthorize("hasAuthority('train:course-kejian:update')")
  @AuditLog(module = "training", action = "course-kejian:update", targetType = "course_kejian")
  public ApiResponse<CourseKejianVO> update(@PathVariable Long id, @Valid @RequestBody CourseKejianUpdateReq req) {
    return ApiResponse.ok(courseKejianApplicationService.update(id, req));
  }

  /** 删除课程课件 */
  @DeleteMapping("/course-kejians/{id}")
  @PreAuthorize("hasAuthority('train:course-kejian:delete')")
  @AuditLog(module = "training", action = "course-kejian:delete", targetType = "course_kejian")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    courseKejianApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
