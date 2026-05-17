package com.guoyu.platform.exam.controller;

import lombok.extern.slf4j.Slf4j;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.exam.dto.ExamWorkTypeCreateReq;
import com.guoyu.platform.exam.dto.ExamWorkTypeQueryReq;
import com.guoyu.platform.exam.dto.ExamWorkTypeUpdateReq;
import com.guoyu.platform.exam.service.ExamWorkTypeApplicationService;
import com.guoyu.platform.exam.vo.ExamWorkTypeVO;
import jakarta.validation.Valid;
import java.util.List;
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
public class ExamWorkTypeController {

  private final ExamWorkTypeApplicationService service;

  public ExamWorkTypeController(ExamWorkTypeApplicationService service) {
    this.service = service;
  }

  @GetMapping("/work-types")
  @PreAuthorize("hasAuthority('exam:work-type:view')")
  public ApiResponse<PageResponse<ExamWorkTypeVO>> page(@Valid @ModelAttribute ExamWorkTypeQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/work-types/tree")
  @PreAuthorize("hasAuthority('exam:work-type:view')")
  public ApiResponse<List<ExamWorkTypeVO>> tree() {
    return ApiResponse.ok(service.tree());
  }

  @GetMapping("/work-types/{id}")
  @PreAuthorize("hasAuthority('exam:work-type:view')")
  public ApiResponse<ExamWorkTypeVO> get(@PathVariable Integer id) {
    return ApiResponse.ok(service.getById(id));
  }

  @PostMapping("/work-types")
  @PreAuthorize("hasAuthority('exam:work-type:create')")
  @AuditLog(module = "exam", action = "work-type:create", targetType = "exam_work_type")
  public ApiResponse<ExamWorkTypeVO> create(@Valid @RequestBody ExamWorkTypeCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  @PutMapping("/work-types/{id}")
  @PreAuthorize("hasAuthority('exam:work-type:update')")
  @AuditLog(module = "exam", action = "work-type:update", targetType = "exam_work_type")
  public ApiResponse<ExamWorkTypeVO> update(@PathVariable Integer id, @Valid @RequestBody ExamWorkTypeUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  @DeleteMapping("/work-types/{id}")
  @PreAuthorize("hasAuthority('exam:work-type:delete')")
  @AuditLog(module = "exam", action = "work-type:delete", targetType = "exam_work_type")
  public ApiResponse<Void> delete(@PathVariable Integer id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
