package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainUserCoursePlanCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserCoursePlanQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserCoursePlanUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainUserCoursePlanService;
import com.guoyu.platform.trainingdepartment.vo.TrainUserCoursePlanVO;
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
 * 用户课程计划管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainUserCoursePlanController {

  private final TrainUserCoursePlanService service;

  public TrainUserCoursePlanController(TrainUserCoursePlanService service) {
    this.service = service;
  }

  /** 分页查询用户课程计划列表 */
  @GetMapping("/user-course-plans")
  @PreAuthorize("hasAuthority('train:user-course-plan:view')")
  public ApiResponse<PageResponse<TrainUserCoursePlanVO>> page(@Valid @ModelAttribute TrainUserCoursePlanQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  /** 根据ID查询用户课程计划详情 */
  @GetMapping("/user-course-plans/{id}")
  @PreAuthorize("hasAuthority('train:user-course-plan:view')")
  public ApiResponse<TrainUserCoursePlanVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  /** 新增用户课程计划 */
  @PostMapping("/user-course-plans")
  @PreAuthorize("hasAuthority('train:user-course-plan:create')")
  @AuditLog(module = "training", action = "user-course-plan:create", targetType = "train_user_course_plan")
  public ApiResponse<TrainUserCoursePlanVO> create(@Valid @RequestBody TrainUserCoursePlanCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  /** 修改用户课程计划 */
  @PutMapping("/user-course-plans/{id}")
  @PreAuthorize("hasAuthority('train:user-course-plan:update')")
  @AuditLog(module = "training", action = "user-course-plan:update", targetType = "train_user_course_plan")
  public ApiResponse<TrainUserCoursePlanVO> update(@PathVariable Long id, @Valid @RequestBody TrainUserCoursePlanUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  /** 删除用户课程计划 */
  @DeleteMapping("/user-course-plans/{id}")
  @PreAuthorize("hasAuthority('train:user-course-plan:delete')")
  @AuditLog(module = "training", action = "user-course-plan:delete", targetType = "train_user_course_plan")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
