package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.SignUpPeopleBatchReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpPeopleSelfCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpPeopleSelfQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpPeopleSelfUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainSignUpCollectionApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainSignUpPeopleSelfApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainSignUpPeopleSelfVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training")
public class TrainSignUpPeopleSelfController {

  private final TrainSignUpPeopleSelfApplicationService trainSignUpPeopleSelfApplicationService;
  private final TrainSignUpCollectionApplicationService collectionService;

  public TrainSignUpPeopleSelfController(
      TrainSignUpPeopleSelfApplicationService trainSignUpPeopleSelfApplicationService,
      TrainSignUpCollectionApplicationService collectionService) {
    this.trainSignUpPeopleSelfApplicationService = trainSignUpPeopleSelfApplicationService;
    this.collectionService = collectionService;
  }

  @GetMapping("/sign-up-people")
  @PreAuthorize("hasAuthority('train:sign-up-people:view')")
  public ApiResponse<PageResponse<TrainSignUpPeopleSelfVO>> page(@Valid @ModelAttribute TrainSignUpPeopleSelfQueryReq req) {
    return ApiResponse.ok(trainSignUpPeopleSelfApplicationService.page(req));
  }

  @GetMapping("/sign-up-people/{id}")
  @PreAuthorize("hasAuthority('train:sign-up-people:view')")
  public ApiResponse<TrainSignUpPeopleSelfVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainSignUpPeopleSelfApplicationService.getById(id));
  }

  @PostMapping("/sign-up-people")
  @PreAuthorize("hasAuthority('train:sign-up-people:create')")
  @AuditLog(module = "training", action = "sign-up-people:create", targetType = "sign-up-people")
  public ApiResponse<TrainSignUpPeopleSelfVO> create(@Valid @RequestBody TrainSignUpPeopleSelfCreateReq req) {
    return ApiResponse.ok(trainSignUpPeopleSelfApplicationService.create(req));
  }

  @PutMapping("/sign-up-people/{id}")
  @PreAuthorize("hasAuthority('train:sign-up-people:update')")
  @AuditLog(module = "training", action = "sign-up-people:update", targetType = "sign-up-people")
  public ApiResponse<TrainSignUpPeopleSelfVO> update(@PathVariable Long id, @Valid @RequestBody TrainSignUpPeopleSelfUpdateReq req) {
    return ApiResponse.ok(trainSignUpPeopleSelfApplicationService.update(id, req));
  }

  @DeleteMapping("/sign-up-people/{id}")
  @PreAuthorize("hasAuthority('train:sign-up-people:delete')")
  @AuditLog(module = "training", action = "sign-up-people:delete", targetType = "sign-up-people")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainSignUpPeopleSelfApplicationService.delete(id);
    return ApiResponse.ok();
  }

  /** 批量审核/删除（对齐 peopleselfoptmoreAction） */
  @PutMapping("/sign-up-people/batch")
  @PreAuthorize("hasAuthority('train:sign-up-people:update')")
  @AuditLog(module = "training", action = "sign-up-people:batch", targetType = "sign-up-people")
  public ApiResponse<Integer> batchUpdate(@Valid @RequestBody SignUpPeopleBatchReq req) {
    return ApiResponse.ok(collectionService.batchUpdatePeople(req.getIds(), req.getField(), req.getValue()));
  }
}
