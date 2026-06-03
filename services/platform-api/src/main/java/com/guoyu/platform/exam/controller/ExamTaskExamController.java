package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.exam.dto.ExamTaskExamAssignExpertReq;
import com.guoyu.platform.exam.dto.ExamTaskExamQueryReq;
import com.guoyu.platform.exam.service.ExamTaskExamApplicationService;
import com.guoyu.platform.exam.vo.ExamTaskExamDailyVO;
import com.guoyu.platform.exam.vo.ExamTaskExamDetailVO;
import com.guoyu.platform.exam.vo.ExamTaskExamVO;
import com.guoyu.platform.exam.vo.PublicDataVO;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/exam")
@Slf4j
public class ExamTaskExamController {

  private final ExamTaskExamApplicationService service;

  public ExamTaskExamController(ExamTaskExamApplicationService service) {
    this.service = service;
  }

  @GetMapping("/task-exams")
  @PreAuthorize("hasAuthority('task:exam-audit:view')")
  public ApiResponse<PageResponse<ExamTaskExamVO>> page(@Valid @ModelAttribute ExamTaskExamQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/task-exams/{id}/details")
  @PreAuthorize("hasAuthority('task:exam-audit:view')")
  public ApiResponse<List<ExamTaskExamDetailVO>> details(@PathVariable Integer id) {
    return ApiResponse.ok(service.getDetails(id));
  }

  @GetMapping("/task-exams/{id}/daily")
  @PreAuthorize("hasAuthority('task:exam-audit:view')")
  public ApiResponse<List<ExamTaskExamDailyVO>> daily(
      @PathVariable Integer id, @RequestParam Integer role) {
    return ApiResponse.ok(service.getDaily(id, role));
  }

  @PostMapping("/task-exams/{id}/assign-expert")
  @PreAuthorize("hasAuthority('task:exam-audit:assign-expert')")
  @AuditLog(module = "exam", action = "task-exam:assign-expert", targetType = "exam_task_exam")
  public ApiResponse<Void> assignExpert(
      @PathVariable Integer id,
      @Valid @RequestBody ExamTaskExamAssignExpertReq req) {
    log.info("POST /exam/task-exams/{}/assign-expert expertId={}, role={}", id, req.expertId(), req.role());
    service.assignExpert(req);
    return ApiResponse.ok();
  }

  @DeleteMapping("/task-exams/{id}")
  @PreAuthorize("hasAuthority('task:exam-audit:delete')")
  @AuditLog(module = "exam", action = "task-exam:delete", targetType = "exam_task_exam")
  public ApiResponse<Void> delete(@PathVariable Integer id) {
    log.info("DELETE /exam/task-exams/{}", id);
    service.delete(id);
    return ApiResponse.ok();
  }

  @GetMapping("/task-exams/public-data")
  @PreAuthorize("hasAuthority('task:exam-audit:view')")
  public ApiResponse<PublicDataVO> publicData() {
    return ApiResponse.ok(service.getPublicData());
  }
}
