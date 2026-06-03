package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.exam.dto.ExpertTaskExamSubmitReq;
import com.guoyu.platform.exam.service.ExpertTaskExamApplicationService;
import com.guoyu.platform.exam.vo.ExamTaskSubmitResultVO;
import com.guoyu.platform.exam.vo.ExpertTaskVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/expert")
@Slf4j
public class ExpertTaskExamController {

  private final ExpertTaskExamApplicationService service;

  public ExpertTaskExamController(ExpertTaskExamApplicationService service) {
    this.service = service;
  }

  @GetMapping("/task-exams")
  @PreAuthorize("hasAuthority('expert:task-exam:view')")
  public ApiResponse<PageResponse<ExpertTaskVO>> myTasks(
      @RequestParam Integer role,
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long pageSize) {
    return ApiResponse.ok(service.myTasks(role, page, pageSize));
  }

  @GetMapping("/task-exams/{id}")
  @PreAuthorize("hasAuthority('expert:task-exam:view')")
  public ApiResponse<ExpertTaskVO> getTask(@PathVariable Integer id) {
    return ApiResponse.ok(service.getTaskDetail(id));
  }

  @PostMapping("/task-exams/{id}/submit")
  @PreAuthorize("hasAuthority('expert:task-exam:submit')")
  @AuditLog(module = "exam", action = "task-exam:submit", targetType = "exam_task_exam_audit_records")
  public ApiResponse<ExamTaskSubmitResultVO> submit(
      @PathVariable Integer id,
      @Valid @RequestBody ExpertTaskExamSubmitReq req) {
    log.info("POST /expert/task-exams/{}/submit", id);
    return ApiResponse.ok(service.submit(req));
  }
}
