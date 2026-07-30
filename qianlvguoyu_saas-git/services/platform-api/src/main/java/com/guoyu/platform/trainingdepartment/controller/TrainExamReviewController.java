package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainExamReviewCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamReviewQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamReviewUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainExamReviewApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainExamReviewVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training")
public class TrainExamReviewController {

  private final TrainExamReviewApplicationService trainExamReviewApplicationService;

  public TrainExamReviewController(TrainExamReviewApplicationService trainExamReviewApplicationService) {
    this.trainExamReviewApplicationService = trainExamReviewApplicationService;
  }

  @GetMapping("/exam-reviews")
  @PreAuthorize("hasAuthority('train:exam-review:view')")
  public ApiResponse<PageResponse<TrainExamReviewVO>> page(@Valid @ModelAttribute TrainExamReviewQueryReq req) {
    return ApiResponse.ok(trainExamReviewApplicationService.page(req));
  }

  @GetMapping("/exam-reviews/{id}")
  @PreAuthorize("hasAuthority('train:exam-review:view')")
  public ApiResponse<TrainExamReviewVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainExamReviewApplicationService.getById(id));
  }

  @PostMapping("/exam-reviews")
  @PreAuthorize("hasAuthority('train:exam-review:create')")
  @AuditLog(module = "training", action = "exam-review:create", targetType = "exam-reviews")
  public ApiResponse<TrainExamReviewVO> create(@Valid @RequestBody TrainExamReviewCreateReq req) {
    return ApiResponse.ok(trainExamReviewApplicationService.create(req));
  }

  @PutMapping("/exam-reviews/{id}")
  @PreAuthorize("hasAuthority('train:exam-review:update')")
  @AuditLog(module = "training", action = "exam-review:update", targetType = "exam-reviews")
  public ApiResponse<TrainExamReviewVO> update(@PathVariable Long id, @Valid @RequestBody TrainExamReviewUpdateReq req) {
    return ApiResponse.ok(trainExamReviewApplicationService.update(id, req));
  }

  @DeleteMapping("/exam-reviews/{id}")
  @PreAuthorize("hasAuthority('train:exam-review:delete')")
  @AuditLog(module = "training", action = "exam-review:delete", targetType = "exam-reviews")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainExamReviewApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
