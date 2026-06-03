package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.exam.dto.ExamTaskExamshicaoAssignExpertReq;
import com.guoyu.platform.exam.dto.ExamTaskExamshicaoQueryReq;
import com.guoyu.platform.exam.service.ExamTaskExamshicaoApplicationService;
import com.guoyu.platform.exam.vo.ExamTaskExamshicaoDailyVO;
import com.guoyu.platform.exam.vo.ExamTaskExamshicaoDetailVO;
import com.guoyu.platform.exam.vo.ExamTaskExamshicaoVO;
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
public class ExamTaskExamshicaoController {

  private final ExamTaskExamshicaoApplicationService service;

  public ExamTaskExamshicaoController(ExamTaskExamshicaoApplicationService service) {
    this.service = service;
  }

  @GetMapping("/task-examshicaos")
  @PreAuthorize("hasAuthority('task:examshicao-audit:view')")
  public ApiResponse<PageResponse<ExamTaskExamshicaoVO>> page(@Valid @ModelAttribute ExamTaskExamshicaoQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/task-examshicaos/{id}/details")
  @PreAuthorize("hasAuthority('task:examshicao-audit:view')")
  public ApiResponse<List<ExamTaskExamshicaoDetailVO>> details(@PathVariable Integer id) {
    return ApiResponse.ok(service.getDetails(id));
  }

  @GetMapping("/task-examshicaos/{id}/daily")
  @PreAuthorize("hasAuthority('task:examshicao-audit:view')")
  public ApiResponse<List<ExamTaskExamshicaoDailyVO>> daily(
      @PathVariable Integer id, @RequestParam Integer role) {
    return ApiResponse.ok(service.getDaily(id, role));
  }

  @PostMapping("/task-examshicaos/{id}/assign-expert")
  @PreAuthorize("hasAuthority('task:examshicao-audit:assign-expert')")
  @AuditLog(module = "exam", action = "task-examshicao:assign-expert", targetType = "exam_task_examshicao")
  public ApiResponse<Void> assignExpert(
      @PathVariable Integer id,
      @Valid @RequestBody ExamTaskExamshicaoAssignExpertReq req) {
    log.info("POST /exam/task-examshicaos/{}/assign-expert", id);
    service.assignExpert(req);
    return ApiResponse.ok();
  }

  @DeleteMapping("/task-examshicaos/{id}")
  @PreAuthorize("hasAuthority('task:examshicao-audit:delete')")
  @AuditLog(module = "exam", action = "task-examshicao:delete", targetType = "exam_task_examshicao")
  public ApiResponse<Void> delete(@PathVariable Integer id) {
    log.info("DELETE /exam/task-examshicaos/{}", id);
    service.delete(id);
    return ApiResponse.ok();
  }

  @GetMapping("/task-examshicaos/public-data")
  @PreAuthorize("hasAuthority('task:examshicao-audit:view')")
  public ApiResponse<PublicDataVO> publicData() {
    return ApiResponse.ok(service.getPublicData());
  }
}
