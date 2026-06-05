package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.exam.dto.ExpertAuditSubmitReq;
import com.guoyu.platform.exam.dto.ExpertTaskQueryReq;
import com.guoyu.platform.exam.service.ExpertAuditApplicationService;
import com.guoyu.platform.exam.vo.ExpertTaskVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/expert")
public class ExpertAuditController {

  private final ExpertAuditApplicationService service;

  public ExpertAuditController(ExpertAuditApplicationService service) {
    this.service = service;
  }

  @GetMapping("/start-tasks")
  @PreAuthorize("hasAuthority('task:audit:start:view')")
  public ApiResponse<PageResponse<ExpertTaskVO>> startTasks(
      @Valid @ModelAttribute ExpertTaskQueryReq req,
      @RequestParam(required = false) String taskType) {
    return ApiResponse.ok(service.pageStartTasks(req, taskType));
  }

  @GetMapping("/end-tasks")
  @PreAuthorize("hasAuthority('task:audit:end:view')")
  public ApiResponse<PageResponse<ExpertTaskVO>> endTasks(
      @Valid @ModelAttribute ExpertTaskQueryReq req,
      @RequestParam(required = false) String taskType) {
    return ApiResponse.ok(service.pageEndTasks(req, taskType));
  }

  @GetMapping("/exam-list")
  @PreAuthorize("hasAnyAuthority('task:audit:start:view','task:audit:end:view')")
  public ApiResponse<List<Map<String, Object>>> examList(@RequestParam Integer taskId, @RequestParam Integer role) {
    return ApiResponse.ok(service.getExamList(taskId, role));
  }

  @PostMapping("/exam-submit")
  @PreAuthorize("hasAnyAuthority('task:audit:start:view','task:audit:end:view')")
  public ApiResponse<Map<String, Object>> examSubmit(@RequestBody ExpertAuditSubmitReq req) {
    return ApiResponse.ok(service.submitExam(req));
  }

  @GetMapping("/examshicao-list")
  @PreAuthorize("hasAnyAuthority('task:audit:start:view','task:audit:end:view')")
  public ApiResponse<Map<String, Object>> examshicaoList(@RequestParam Integer taskId, @RequestParam Integer role) {
    return ApiResponse.ok(service.getExamshicaoList(taskId, role));
  }

  @PostMapping("/examshicao-submit")
  @PreAuthorize("hasAnyAuthority('task:audit:start:view','task:audit:end:view')")
  public ApiResponse<Map<String, Object>> examshicaoSubmit(@RequestBody ExpertAuditSubmitReq req) {
    return ApiResponse.ok(service.submitExamshicao(req));
  }

  @GetMapping("/examsub-list")
  @PreAuthorize("hasAnyAuthority('task:audit:start:view','task:audit:end:view')")
  public ApiResponse<List<Map<String, Object>>> examsubList(@RequestParam Integer taskId, @RequestParam Integer role) {
    return ApiResponse.ok(service.getExamsubList(taskId, role));
  }

  @PostMapping("/examsub-submit")
  @PreAuthorize("hasAnyAuthority('task:audit:start:view','task:audit:end:view')")
  public ApiResponse<Map<String, Object>> examsubSubmit(@RequestBody ExpertAuditSubmitReq req) {
    return ApiResponse.ok(service.submitExamsub(req));
  }

  @GetMapping("/ximu-list")
  @PreAuthorize("hasAnyAuthority('task:audit:start:view','task:audit:end:view')")
  public ApiResponse<Map<String, Object>> ximuList(@RequestParam Integer taskId, @RequestParam Integer role) {
    return ApiResponse.ok(service.getXimuList(taskId, role));
  }

  @PostMapping("/ximu-submit")
  @PreAuthorize("hasAnyAuthority('task:audit:start:view','task:audit:end:view')")
  public ApiResponse<Map<String, Object>> ximuSubmit(@RequestBody ExpertAuditSubmitReq req) {
    return ApiResponse.ok(service.submitXimu(req));
  }
}
