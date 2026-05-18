package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainUserCoursePaperCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserCoursePaperQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserCoursePaperUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainUserCoursePaperService;
import com.guoyu.platform.trainingdepartment.vo.TrainUserCoursePaperVO;
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
 * 用户已获得证书管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainUserCoursePaperController {

  private final TrainUserCoursePaperService service;

  public TrainUserCoursePaperController(TrainUserCoursePaperService service) {
    this.service = service;
  }

  /** 分页查询用户已获得证书列表 */
  @GetMapping("/user-course-papers")
  @PreAuthorize("hasAuthority('train:user-course-paper:view')")
  public ApiResponse<PageResponse<TrainUserCoursePaperVO>> page(@Valid @ModelAttribute TrainUserCoursePaperQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  /** 根据ID查询用户已获得证书详情 */
  @GetMapping("/user-course-papers/{id}")
  @PreAuthorize("hasAuthority('train:user-course-paper:view')")
  public ApiResponse<TrainUserCoursePaperVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  /** 新增用户已获得证书 */
  @PostMapping("/user-course-papers")
  @PreAuthorize("hasAuthority('train:user-course-paper:create')")
  @AuditLog(module = "training", action = "user-course-paper:create", targetType = "train_user_course_paper")
  public ApiResponse<TrainUserCoursePaperVO> create(@Valid @RequestBody TrainUserCoursePaperCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  /** 修改用户已获得证书 */
  @PutMapping("/user-course-papers/{id}")
  @PreAuthorize("hasAuthority('train:user-course-paper:update')")
  @AuditLog(module = "training", action = "user-course-paper:update", targetType = "train_user_course_paper")
  public ApiResponse<TrainUserCoursePaperVO> update(@PathVariable Long id, @Valid @RequestBody TrainUserCoursePaperUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  /** 删除用户已获得证书 */
  @DeleteMapping("/user-course-papers/{id}")
  @PreAuthorize("hasAuthority('train:user-course-paper:delete')")
  @AuditLog(module = "training", action = "user-course-paper:delete", targetType = "train_user_course_paper")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
