package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainExamRoomListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamRoomListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamRoomListUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainExamRoomListApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainExamRoomListVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training")
public class TrainExamRoomListController {

  private final TrainExamRoomListApplicationService trainExamRoomListApplicationService;

  public TrainExamRoomListController(TrainExamRoomListApplicationService trainExamRoomListApplicationService) {
    this.trainExamRoomListApplicationService = trainExamRoomListApplicationService;
  }

  @GetMapping("/exam-rooms")
  @PreAuthorize("hasAuthority('train:exam-room:view')")
  public ApiResponse<PageResponse<TrainExamRoomListVO>> page(@Valid @ModelAttribute TrainExamRoomListQueryReq req) {
    return ApiResponse.ok(trainExamRoomListApplicationService.page(req));
  }

  @GetMapping("/exam-rooms/{id}")
  @PreAuthorize("hasAuthority('train:exam-room:view')")
  public ApiResponse<TrainExamRoomListVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainExamRoomListApplicationService.getById(id));
  }

  @PostMapping("/exam-rooms")
  @PreAuthorize("hasAuthority('train:exam-room:create')")
  @AuditLog(module = "training", action = "exam-room:create", targetType = "exam-rooms")
  public ApiResponse<TrainExamRoomListVO> create(@Valid @RequestBody TrainExamRoomListCreateReq req) {
    return ApiResponse.ok(trainExamRoomListApplicationService.create(req));
  }

  @PutMapping("/exam-rooms/{id}")
  @PreAuthorize("hasAuthority('train:exam-room:update')")
  @AuditLog(module = "training", action = "exam-room:update", targetType = "exam-rooms")
  public ApiResponse<TrainExamRoomListVO> update(@PathVariable Long id, @Valid @RequestBody TrainExamRoomListUpdateReq req) {
    return ApiResponse.ok(trainExamRoomListApplicationService.update(id, req));
  }

  @DeleteMapping("/exam-rooms/{id}")
  @PreAuthorize("hasAuthority('train:exam-room:delete')")
  @AuditLog(module = "training", action = "exam-room:delete", targetType = "exam-rooms")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainExamRoomListApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
