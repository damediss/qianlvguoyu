package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.exam.dto.ExamCourseCreateReq;
import com.guoyu.platform.exam.dto.ExamCourseQueryReq;
import com.guoyu.platform.exam.dto.ExamCourseUpdateReq;
import com.guoyu.platform.exam.service.ExamCourseApplicationService;
import com.guoyu.platform.exam.vo.ExamCourseVO;
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
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/exam")
@Slf4j
public class ExamCourseController {

  private final ExamCourseApplicationService service;

  public ExamCourseController(ExamCourseApplicationService service) {
    this.service = service;
  }

  @GetMapping("/courses")
  @PreAuthorize("hasAuthority('exam:course:view')")
  public ApiResponse<PageResponse<ExamCourseVO>> page(@Valid @ModelAttribute ExamCourseQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/courses/{id}")
  @PreAuthorize("hasAuthority('exam:course:view')")
  public ApiResponse<ExamCourseVO> get(@PathVariable Integer id) {
    return ApiResponse.ok(service.getById(id));
  }

  @PostMapping("/courses")
  @PreAuthorize("hasAuthority('exam:course:create')")
  @AuditLog(module = "exam", action = "course:create", targetType = "exam_course")
  public ApiResponse<ExamCourseVO> create(@Valid @RequestBody ExamCourseCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  @PutMapping("/courses/{id}")
  @PreAuthorize("hasAuthority('exam:course:update')")
  @AuditLog(module = "exam", action = "course:update", targetType = "exam_course")
  public ApiResponse<ExamCourseVO> update(@PathVariable Integer id, @Valid @RequestBody ExamCourseUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  @DeleteMapping("/courses/{id}")
  @PreAuthorize("hasAuthority('exam:course:delete')")
  @AuditLog(module = "exam", action = "course:delete", targetType = "exam_course")
  public ApiResponse<Void> delete(@PathVariable Integer id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}