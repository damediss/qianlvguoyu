package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.exam.dto.FaqModelConfigCreateReq;
import com.guoyu.platform.exam.dto.FaqModelConfigQueryReq;
import com.guoyu.platform.exam.dto.FaqModelConfigUpdateReq;
import com.guoyu.platform.exam.service.FaqModelConfigApplicationService;
import com.guoyu.platform.exam.vo.FaqModelConfigVO;
import com.guoyu.platform.common.api.PageResponse;
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

/** LLM 模型配置 CRUD */
@Validated
@RestController
@RequestMapping("/exam/faq/model-config")
@Slf4j
public class ExamFaqModelConfigController {

  private final FaqModelConfigApplicationService service;

  public ExamFaqModelConfigController(FaqModelConfigApplicationService service) {
    this.service = service;
  }

  @GetMapping("/page")
  @PreAuthorize("hasAuthority('exam:faq:config')")
  public ApiResponse<PageResponse<FaqModelConfigVO>> page(@Valid @ModelAttribute FaqModelConfigQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('exam:faq:config')")
  public ApiResponse<FaqModelConfigVO> get(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('exam:faq:config')")
  @AuditLog(module = "exam", action = "faq:model-config:create", targetType = "exam_faq_model_config")
  public ApiResponse<FaqModelConfigVO> create(@Valid @RequestBody FaqModelConfigCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('exam:faq:config')")
  @AuditLog(module = "exam", action = "faq:model-config:update", targetType = "exam_faq_model_config")
  public ApiResponse<FaqModelConfigVO> update(@PathVariable Long id, @Valid @RequestBody FaqModelConfigUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('exam:faq:config')")
  @AuditLog(module = "exam", action = "faq:model-config:delete", targetType = "exam_faq_model_config")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
