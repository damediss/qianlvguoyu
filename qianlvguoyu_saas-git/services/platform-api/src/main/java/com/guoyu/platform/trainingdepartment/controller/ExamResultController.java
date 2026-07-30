package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.ExamResultCreateReq;
import com.guoyu.platform.trainingdepartment.dto.ExamResultQueryReq;
import com.guoyu.platform.trainingdepartment.dto.ExamResultUpdateReq;
import com.guoyu.platform.trainingdepartment.service.ExamResultApplicationService;
import com.guoyu.platform.trainingdepartment.vo.ExamResultVO;
import jakarta.validation.Valid;
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

/**
 * 考试结果管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class ExamResultController {

  private final ExamResultApplicationService examResultApplicationService;

  public ExamResultController(ExamResultApplicationService examResultApplicationService) {
    this.examResultApplicationService = examResultApplicationService;
  }

  /** 分页查询考试结果列表 */
  @GetMapping("/exam-results")
  @PreAuthorize("hasAuthority('train:exam-result:view')")
  public ApiResponse<PageResponse<ExamResultVO>> page(@Valid @ModelAttribute ExamResultQueryReq req) {
    return ApiResponse.ok(examResultApplicationService.page(req));
  }

  /** 根据ID查询考试结果详情 */
  @GetMapping("/exam-results/{id}")
  @PreAuthorize("hasAuthority('train:exam-result:view')")
  public ApiResponse<ExamResultVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(examResultApplicationService.getById(id));
  }

  /** 新增考试结果 */
  @PostMapping("/exam-results")
  @PreAuthorize("hasAuthority('train:exam-result:create')")
  @AuditLog(module = "training", action = "exam-result:create", targetType = "exam_result")
  public ApiResponse<ExamResultVO> create(@Valid @RequestBody ExamResultCreateReq req) {
    return ApiResponse.ok(examResultApplicationService.create(req));
  }

  /** 修改考试结果 */
  @PutMapping("/exam-results/{id}")
  @PreAuthorize("hasAuthority('train:exam-result:update')")
  @AuditLog(module = "training", action = "exam-result:update", targetType = "exam_result")
  public ApiResponse<ExamResultVO> update(@PathVariable Long id, @Valid @RequestBody ExamResultUpdateReq req) {
    return ApiResponse.ok(examResultApplicationService.update(id, req));
  }

  /** 删除考试结果 */
  @DeleteMapping("/exam-results/{id}")
  @PreAuthorize("hasAuthority('train:exam-result:delete')")
  @AuditLog(module = "training", action = "exam-result:delete", targetType = "exam_result")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    examResultApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
