package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.exam.dto.ExamUserAuthCourseCreateReq;
import com.guoyu.platform.exam.dto.ExamUserAuthCourseQueryReq;
import com.guoyu.platform.exam.service.ExamUserAuthCourseApplicationService;
import com.guoyu.platform.exam.vo.ExamUserAuthCourseVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/exam")
@Slf4j
public class ExamUserAuthCourseController {

  private final ExamUserAuthCourseApplicationService service;

  public ExamUserAuthCourseController(ExamUserAuthCourseApplicationService service) {
    this.service = service;
  }

  @GetMapping("/user-auth-courses")
  @PreAuthorize("hasAuthority('exam:user-auth:view')")
  public ApiResponse<PageResponse<ExamUserAuthCourseVO>> page(@Valid @ModelAttribute ExamUserAuthCourseQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/user-auth-courses/detail")
  @PreAuthorize("hasAuthority('exam:user-auth:view')")
  public ApiResponse<ExamUserAuthCourseVO> get(
      @RequestParam String realname,
      @RequestParam String idCard) {
    return ApiResponse.ok(service.getByKey(realname, idCard));
  }

  @PostMapping("/user-auth-courses")
  @PreAuthorize("hasAuthority('exam:user-auth:create')")
  @AuditLog(module = "exam", action = "user-auth:create", targetType = "exam_user_auth_course")
  public ApiResponse<ExamUserAuthCourseVO> create(@Valid @RequestBody ExamUserAuthCourseCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  @PutMapping("/user-auth-courses/{realname}/{idCard}")
  @PreAuthorize("hasAuthority('exam:user-auth:update')")
  @AuditLog(module = "exam", action = "user-auth:update", targetType = "exam_user_auth_course")
  public ApiResponse<ExamUserAuthCourseVO> update(
      @PathVariable String realname,
      @PathVariable String idCard,
      @Valid @RequestBody ExamUserAuthCourseCreateReq req) {
    return ApiResponse.ok(service.update(realname, idCard, req));
  }

  @DeleteMapping("/user-auth-courses/{realname}/{idCard}")
  @PreAuthorize("hasAuthority('exam:user-auth:delete')")
  @AuditLog(module = "exam", action = "user-auth:delete", targetType = "exam_user_auth_course")
  public ApiResponse<Void> delete(
      @PathVariable String realname,
      @PathVariable String idCard) {
    service.delete(realname, idCard);
    return ApiResponse.ok();
  }
}