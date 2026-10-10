package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainExamSessionCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamSessionQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamSessionUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainExamSessionApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainExamSessionVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training")
public class TrainExamSessionController {

  private final TrainExamSessionApplicationService trainExamSessionApplicationService;

  public TrainExamSessionController(TrainExamSessionApplicationService trainExamSessionApplicationService) {
    this.trainExamSessionApplicationService = trainExamSessionApplicationService;
  }

  @GetMapping("/exam-sessions")
  @PreAuthorize("hasAuthority('train:exam-session:view')")
  public ApiResponse<PageResponse<TrainExamSessionVO>> page(@Valid @ModelAttribute TrainExamSessionQueryReq req) {
    return ApiResponse.ok(trainExamSessionApplicationService.page(req));
  }

  @GetMapping("/exam-sessions/{id}")
  @PreAuthorize("hasAuthority('train:exam-session:view')")
  public ApiResponse<TrainExamSessionVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainExamSessionApplicationService.getById(id));
  }

  @PostMapping("/exam-sessions")
  @PreAuthorize("hasAuthority('train:exam-session:create')")
  @AuditLog(module = "training", action = "exam-session:create", targetType = "exam-sessions")
  public ApiResponse<TrainExamSessionVO> create(@Valid @RequestBody TrainExamSessionCreateReq req) {
    return ApiResponse.ok(trainExamSessionApplicationService.create(req));
  }

  @PutMapping("/exam-sessions/{id}")
  @PreAuthorize("hasAuthority('train:exam-session:update')")
  @AuditLog(module = "training", action = "exam-session:update", targetType = "exam-sessions")
  public ApiResponse<TrainExamSessionVO> update(@PathVariable Long id, @Valid @RequestBody TrainExamSessionUpdateReq req) {
    return ApiResponse.ok(trainExamSessionApplicationService.update(id, req));
  }

  @DeleteMapping("/exam-sessions/{id}")
  @PreAuthorize("hasAuthority('train:exam-session:delete')")
  @AuditLog(module = "training", action = "exam-session:delete", targetType = "exam-sessions")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainExamSessionApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
