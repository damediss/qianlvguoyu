package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.exam.dto.ExamKnowledgeRateCreateReq;
import com.guoyu.platform.exam.dto.ExamKnowledgeRateQueryReq;
import com.guoyu.platform.exam.dto.ExamKnowledgeRateUpdateReq;
import com.guoyu.platform.exam.service.ExamKnowledgeRateApplicationService;
import com.guoyu.platform.exam.vo.ExamKnowledgeRateVO;
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
public class ExamKnowledgeRateController {

  private final ExamKnowledgeRateApplicationService service;

  public ExamKnowledgeRateController(ExamKnowledgeRateApplicationService service) {
    this.service = service;
  }

  @GetMapping("/knowledge-rates")
  @PreAuthorize("hasAuthority('exam:knowledge-rate:view')")
  public ApiResponse<PageResponse<ExamKnowledgeRateVO>> page(
      @Valid @ModelAttribute ExamKnowledgeRateQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/knowledge-rates/list")
  @PreAuthorize("hasAuthority('exam:knowledge-rate:view')")
  public ApiResponse<List<ExamKnowledgeRateVO>> list(
      @RequestParam(required = false) Integer courseId) {
    return ApiResponse.ok(service.list(courseId));
  }

  @GetMapping("/knowledge-rates/{id}")
  @PreAuthorize("hasAuthority('exam:knowledge-rate:view')")
  public ApiResponse<ExamKnowledgeRateVO> get(@PathVariable Integer id) {
    return ApiResponse.ok(service.getById(id));
  }

  @PostMapping("/knowledge-rates")
  @PreAuthorize("hasAuthority('exam:knowledge-rate:create')")
  @AuditLog(module = "exam", action = "knowledge-rate:create", targetType = "exam_knowledge_rate")
  public ApiResponse<ExamKnowledgeRateVO> create(
      @Valid @RequestBody ExamKnowledgeRateCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  @PutMapping("/knowledge-rates/{id}")
  @PreAuthorize("hasAuthority('exam:knowledge-rate:update')")
  @AuditLog(module = "exam", action = "knowledge-rate:update", targetType = "exam_knowledge_rate")
  public ApiResponse<ExamKnowledgeRateVO> update(
      @PathVariable Integer id,
      @Valid @RequestBody ExamKnowledgeRateUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  @DeleteMapping("/knowledge-rates/{id}")
  @PreAuthorize("hasAuthority('exam:knowledge-rate:delete')")
  @AuditLog(module = "exam", action = "knowledge-rate:delete", targetType = "exam_knowledge_rate")
  public ApiResponse<Void> delete(@PathVariable Integer id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
