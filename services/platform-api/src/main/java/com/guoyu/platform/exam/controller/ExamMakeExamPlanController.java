package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.exam.dto.ExamMakeExamPlanCreateReq;
import com.guoyu.platform.exam.dto.ExamMakeExamPlanQueryReq;
import com.guoyu.platform.exam.dto.ExamMakeExamPlanUpdateReq;
import com.guoyu.platform.exam.service.ExamMakeExamPlanApplicationService;
import com.guoyu.platform.exam.vo.ExamMakeExamPlanVO;
import jakarta.validation.Valid;
import java.util.List;
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
public class ExamMakeExamPlanController {

  private final ExamMakeExamPlanApplicationService service;

  public ExamMakeExamPlanController(ExamMakeExamPlanApplicationService service) {
    this.service = service;
  }

  @GetMapping("/make-exam-plans")
  @PreAuthorize("hasAuthority('exam:make-plan:view')")
  public ApiResponse<PageResponse<ExamMakeExamPlanVO>> page(
      @Valid @ModelAttribute ExamMakeExamPlanQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/make-exam-plans/list")
  @PreAuthorize("hasAuthority('exam:make-plan:view')")
  public ApiResponse<List<ExamMakeExamPlanVO>> list(
      @RequestParam(required = false) Integer courseId,
      @RequestParam(required = false) Integer examType) {
    return ApiResponse.ok(service.list(courseId, examType));
  }

  @GetMapping("/make-exam-plans/{id}")
  @PreAuthorize("hasAuthority('exam:make-plan:view')")
  public ApiResponse<ExamMakeExamPlanVO> get(@PathVariable Integer id) {
    return ApiResponse.ok(service.getById(id));
  }

  @PostMapping("/make-exam-plans")
  @PreAuthorize("hasAuthority('exam:make-plan:create')")
  @AuditLog(module = "exam", action = "make-plan:create", targetType = "exam_make_exam_plan")
  public ApiResponse<ExamMakeExamPlanVO> create(
      @Valid @RequestBody ExamMakeExamPlanCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  @PutMapping("/make-exam-plans/{id}")
  @PreAuthorize("hasAuthority('exam:make-plan:update')")
  @AuditLog(module = "exam", action = "make-plan:update", targetType = "exam_make_exam_plan")
  public ApiResponse<ExamMakeExamPlanVO> update(
      @PathVariable Integer id,
      @Valid @RequestBody ExamMakeExamPlanUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  @DeleteMapping("/make-exam-plans/{id}")
  @PreAuthorize("hasAuthority('exam:make-plan:delete')")
  @AuditLog(module = "exam", action = "make-plan:delete", targetType = "exam_make_exam_plan")
  public ApiResponse<Void> delete(@PathVariable Integer id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
