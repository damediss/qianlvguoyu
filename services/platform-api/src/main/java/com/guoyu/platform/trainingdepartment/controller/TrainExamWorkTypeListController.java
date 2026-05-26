package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainExamWorkTypeListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamWorkTypeListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamWorkTypeListUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainExamWorkTypeListApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainExamWorkTypeListVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training")
public class TrainExamWorkTypeListController {

  private final TrainExamWorkTypeListApplicationService trainExamWorkTypeListApplicationService;

  public TrainExamWorkTypeListController(TrainExamWorkTypeListApplicationService trainExamWorkTypeListApplicationService) {
    this.trainExamWorkTypeListApplicationService = trainExamWorkTypeListApplicationService;
  }

  @GetMapping("/exam-work-types")
  @PreAuthorize("hasAuthority('train:exam-work-type:view')")
  public ApiResponse<PageResponse<TrainExamWorkTypeListVO>> page(@Valid @ModelAttribute TrainExamWorkTypeListQueryReq req) {
    return ApiResponse.ok(trainExamWorkTypeListApplicationService.page(req));
  }

  @GetMapping("/exam-work-types/{id}")
  @PreAuthorize("hasAuthority('train:exam-work-type:view')")
  public ApiResponse<TrainExamWorkTypeListVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainExamWorkTypeListApplicationService.getById(id));
  }

  @PostMapping("/exam-work-types")
  @PreAuthorize("hasAuthority('train:exam-work-type:create')")
  @AuditLog(module = "training", action = "exam-work-type:create", targetType = "exam-work-types")
  public ApiResponse<TrainExamWorkTypeListVO> create(@Valid @RequestBody TrainExamWorkTypeListCreateReq req) {
    return ApiResponse.ok(trainExamWorkTypeListApplicationService.create(req));
  }

  @PutMapping("/exam-work-types/{id}")
  @PreAuthorize("hasAuthority('train:exam-work-type:update')")
  @AuditLog(module = "training", action = "exam-work-type:update", targetType = "exam-work-types")
  public ApiResponse<TrainExamWorkTypeListVO> update(@PathVariable Long id, @Valid @RequestBody TrainExamWorkTypeListUpdateReq req) {
    return ApiResponse.ok(trainExamWorkTypeListApplicationService.update(id, req));
  }

  @DeleteMapping("/exam-work-types/{id}")
  @PreAuthorize("hasAuthority('train:exam-work-type:delete')")
  @AuditLog(module = "training", action = "exam-work-type:delete", targetType = "exam-work-types")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainExamWorkTypeListApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
