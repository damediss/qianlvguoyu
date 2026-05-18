package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainExamListService;
import com.guoyu.platform.trainingdepartment.vo.TrainExamListVO;
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
 * 考试计划管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainExamListController {

  private final TrainExamListService service;

  public TrainExamListController(TrainExamListService service) {
    this.service = service;
  }

  /** 分页查询考试计划 */
  @GetMapping("/exam-lists")
  @PreAuthorize("hasAuthority('train:exam-list:view')")
  public ApiResponse<PageResponse<TrainExamListVO>> page(@Valid @ModelAttribute TrainExamListQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  /** 根据ID查询考试计划 */
  @GetMapping("/exam-lists/{id}")
  @PreAuthorize("hasAuthority('train:exam-list:view')")
  public ApiResponse<TrainExamListVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  /** 新增考试计划 */
  @PostMapping("/exam-lists")
  @PreAuthorize("hasAuthority('train:exam-list:create')")
  @AuditLog(module = "training", action = "exam-list:create", targetType = "train_exam_list")
  public ApiResponse<TrainExamListVO> create(@Valid @RequestBody TrainExamListCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  /** 修改考试计划 */
  @PutMapping("/exam-lists/{id}")
  @PreAuthorize("hasAuthority('train:exam-list:update')")
  @AuditLog(module = "training", action = "exam-list:update", targetType = "train_exam_list")
  public ApiResponse<TrainExamListVO> update(@PathVariable Long id, @Valid @RequestBody TrainExamListUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  /** 删除考试计划 */
  @DeleteMapping("/exam-lists/{id}")
  @PreAuthorize("hasAuthority('train:exam-list:delete')")
  @AuditLog(module = "training", action = "exam-list:delete", targetType = "train_exam_list")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
