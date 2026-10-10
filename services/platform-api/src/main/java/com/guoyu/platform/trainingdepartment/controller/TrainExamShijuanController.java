package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainExamShijuanCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamShijuanQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamShijuanUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainExamShijuanApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainExamShijuanVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training")
public class TrainExamShijuanController {

  private final TrainExamShijuanApplicationService trainExamShijuanApplicationService;

  public TrainExamShijuanController(TrainExamShijuanApplicationService trainExamShijuanApplicationService) {
    this.trainExamShijuanApplicationService = trainExamShijuanApplicationService;
  }

  @GetMapping("/exam-shijuans")
  @PreAuthorize("hasAuthority('train:exam-shijuan:view')")
  public ApiResponse<PageResponse<TrainExamShijuanVO>> page(@Valid @ModelAttribute TrainExamShijuanQueryReq req) {
    return ApiResponse.ok(trainExamShijuanApplicationService.page(req));
  }

  @GetMapping("/exam-shijuans/{id}")
  @PreAuthorize("hasAuthority('train:exam-shijuan:view')")
  public ApiResponse<TrainExamShijuanVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainExamShijuanApplicationService.getById(id));
  }

  @PostMapping("/exam-shijuans")
  @PreAuthorize("hasAuthority('train:exam-shijuan:create')")
  @AuditLog(module = "training", action = "exam-shijuan:create", targetType = "exam-shijuans")
  public ApiResponse<TrainExamShijuanVO> create(@Valid @RequestBody TrainExamShijuanCreateReq req) {
    return ApiResponse.ok(trainExamShijuanApplicationService.create(req));
  }

  @PutMapping("/exam-shijuans/{id}")
  @PreAuthorize("hasAuthority('train:exam-shijuan:update')")
  @AuditLog(module = "training", action = "exam-shijuan:update", targetType = "exam-shijuans")
  public ApiResponse<TrainExamShijuanVO> update(@PathVariable Long id, @Valid @RequestBody TrainExamShijuanUpdateReq req) {
    return ApiResponse.ok(trainExamShijuanApplicationService.update(id, req));
  }

  @DeleteMapping("/exam-shijuans/{id}")
  @PreAuthorize("hasAuthority('train:exam-shijuan:delete')")
  @AuditLog(module = "training", action = "exam-shijuan:delete", targetType = "exam-shijuans")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainExamShijuanApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
