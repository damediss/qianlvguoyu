package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.exam.dto.FaqWeKnoraConfigSaveReq;
import com.guoyu.platform.exam.service.FaqWeknoraConfigApplicationService;
import com.guoyu.platform.exam.vo.FaqWeknoraConfigVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** WeKnora 集成配置 — 单租户一条记录 upsert 语义 */
@Validated
@RestController
@RequestMapping("/exam/faq/weknora-config")
@Slf4j
public class ExamFaqWeknoraConfigController {

  private final FaqWeknoraConfigApplicationService service;

  public ExamFaqWeknoraConfigController(FaqWeknoraConfigApplicationService service) {
    this.service = service;
  }

  /** 查询当前租户配置 */
  @GetMapping
  @PreAuthorize("hasAuthority('exam:faq:config')")
  public ApiResponse<FaqWeknoraConfigVO> getConfig() {
    return ApiResponse.ok(service.getConfig());
  }

  /** 保存/更新配置 */
  @PostMapping
  @PreAuthorize("hasAuthority('exam:faq:config')")
  @AuditLog(module = "exam", action = "faq:weknora-config:save", targetType = "exam_faq_weknora_config")
  public ApiResponse<FaqWeknoraConfigVO> save(@Valid @RequestBody FaqWeKnoraConfigSaveReq req) {
    return ApiResponse.ok(service.save(req));
  }
}
