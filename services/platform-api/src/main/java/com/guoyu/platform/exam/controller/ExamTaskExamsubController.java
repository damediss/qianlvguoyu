package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.exam.dto.ExamTaskExamsubAssignExpertReq;
import com.guoyu.platform.exam.dto.ExamTaskExamsubQueryReq;
import com.guoyu.platform.exam.service.ExamTaskExamsubApplicationService;
import com.guoyu.platform.exam.vo.ExamTaskExamsubDailyVO;
import com.guoyu.platform.exam.vo.ExamTaskExamsubDetailVO;
import com.guoyu.platform.exam.vo.ExamTaskExamsubVO;
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
public class ExamTaskExamsubController {

  private final ExamTaskExamsubApplicationService service;

  public ExamTaskExamsubController(ExamTaskExamsubApplicationService service) {
    this.service = service;
  }

  @GetMapping("/task-examsubs")
  @PreAuthorize("hasAuthority('task:examsub-audit:view')")
  public ApiResponse<PageResponse<ExamTaskExamsubVO>> page(@Valid @ModelAttribute ExamTaskExamsubQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/task-examsubs/{id}/details")
  @PreAuthorize("hasAuthority('task:examsub-audit:view')")
  public ApiResponse<List<ExamTaskExamsubDetailVO>> details(@PathVariable Integer id) {
    return ApiResponse.ok(service.getDetails(id));
  }

  @GetMapping("/task-examsubs/{id}/daily")
  @PreAuthorize("hasAuthority('task:examsub-audit:view')")
  public ApiResponse<List<ExamTaskExamsubDailyVO>> daily(
      @PathVariable Integer id, @RequestParam Integer role) {
    return ApiResponse.ok(service.getDaily(id, role));
  }

  @PostMapping("/task-examsubs/{id}/assign-expert")
  @PreAuthorize("hasAuthority('task:examsub-audit:assign-expert')")
  @AuditLog(module = "exam", action = "task-examsub:assign-expert", targetType = "exam_task_examsub")
  public ApiResponse<Void> assignExpert(
      @PathVariable Integer id,
      @Valid @RequestBody ExamTaskExamsubAssignExpertReq req) {
    log.info("POST /exam/task-examsubs/{}/assign-expert", id);
    service.assignExpert(req);
    return ApiResponse.ok();
  }

  @DeleteMapping("/task-examsubs/{id}")
  @PreAuthorize("hasAuthority('task:examsub-audit:delete')")
  @AuditLog(module = "exam", action = "task-examsub:delete", targetType = "exam_task_examsub")
  public ApiResponse<Void> delete(@PathVariable Integer id) {
    log.info("DELETE /exam/task-examsubs/{}", id);
    service.delete(id);
    return ApiResponse.ok();
  }

  @GetMapping("/task-examsubs/public-data")
  @PreAuthorize("hasAuthority('task:examsub-audit:view')")
  public ApiResponse<PublicDataVO> publicData() {
    return ApiResponse.ok(service.getPublicData());
  }
}
