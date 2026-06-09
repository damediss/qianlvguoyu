package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.exam.dto.ExamExamDetailCreateReq;
import com.guoyu.platform.exam.dto.ExamExamDetailQueryReq;
import com.guoyu.platform.exam.dto.ExamExamDetailUpdateReq;
import com.guoyu.platform.exam.service.ExamExamDetailApplicationService;
import com.guoyu.platform.exam.vo.ExamExamDetailVO;
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
public class ExamExamDetailController {

  private final ExamExamDetailApplicationService service;

  public ExamExamDetailController(ExamExamDetailApplicationService service) {
    this.service = service;
  }

  @GetMapping("/exam-details")
  @PreAuthorize("hasAuthority('exam:question:view')")
  public ApiResponse<PageResponse<ExamExamDetailVO>> page(
      @Valid @ModelAttribute ExamExamDetailQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/exam-details/{id}")
  @PreAuthorize("hasAuthority('exam:question:view')")
  public ApiResponse<ExamExamDetailVO> get(@PathVariable Integer id) {
    return ApiResponse.ok(service.getById(id));
  }

  @PostMapping("/exam-details")
  @PreAuthorize("hasAuthority('exam:question:create')")
  @AuditLog(module = "exam", action = "question:create", targetType = "exam_exam_detail")
  public ApiResponse<ExamExamDetailVO> create(
      @Valid @RequestBody ExamExamDetailCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  @PutMapping("/exam-details/{id}")
  @PreAuthorize("hasAuthority('exam:question:update')")
  @AuditLog(module = "exam", action = "question:update", targetType = "exam_exam_detail")
  public ApiResponse<ExamExamDetailVO> update(
      @PathVariable Integer id,
      @Valid @RequestBody ExamExamDetailUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  @DeleteMapping("/exam-details/{id}")
  @PreAuthorize("hasAuthority('exam:question:delete')")
  @AuditLog(module = "exam", action = "question:delete", targetType = "exam_exam_detail")
  public ApiResponse<Void> delete(@PathVariable Integer id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
