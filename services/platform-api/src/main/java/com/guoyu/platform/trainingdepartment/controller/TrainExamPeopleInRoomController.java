package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainExamPeopleInRoomCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamPeopleInRoomQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamPeopleInRoomUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainExamPeopleInRoomApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainExamPeopleInRoomVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training")
public class TrainExamPeopleInRoomController {

  private final TrainExamPeopleInRoomApplicationService trainExamPeopleInRoomApplicationService;

  public TrainExamPeopleInRoomController(TrainExamPeopleInRoomApplicationService trainExamPeopleInRoomApplicationService) {
    this.trainExamPeopleInRoomApplicationService = trainExamPeopleInRoomApplicationService;
  }

  @GetMapping("/exam-people-rooms")
  @PreAuthorize("hasAuthority('train:exam-people-room:view')")
  public ApiResponse<PageResponse<TrainExamPeopleInRoomVO>> page(@Valid @ModelAttribute TrainExamPeopleInRoomQueryReq req) {
    return ApiResponse.ok(trainExamPeopleInRoomApplicationService.page(req));
  }

  @GetMapping("/exam-people-rooms/{id}")
  @PreAuthorize("hasAuthority('train:exam-people-room:view')")
  public ApiResponse<TrainExamPeopleInRoomVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainExamPeopleInRoomApplicationService.getById(id));
  }

  @PostMapping("/exam-people-rooms")
  @PreAuthorize("hasAuthority('train:exam-people-room:create')")
  @AuditLog(module = "training", action = "exam-people-room:create", targetType = "exam-people-rooms")
  public ApiResponse<TrainExamPeopleInRoomVO> create(@Valid @RequestBody TrainExamPeopleInRoomCreateReq req) {
    return ApiResponse.ok(trainExamPeopleInRoomApplicationService.create(req));
  }

  @PutMapping("/exam-people-rooms/{id}")
  @PreAuthorize("hasAuthority('train:exam-people-room:update')")
  @AuditLog(module = "training", action = "exam-people-room:update", targetType = "exam-people-rooms")
  public ApiResponse<TrainExamPeopleInRoomVO> update(@PathVariable Long id, @Valid @RequestBody TrainExamPeopleInRoomUpdateReq req) {
    return ApiResponse.ok(trainExamPeopleInRoomApplicationService.update(id, req));
  }

  @DeleteMapping("/exam-people-rooms/{id}")
  @PreAuthorize("hasAuthority('train:exam-people-room:delete')")
  @AuditLog(module = "training", action = "exam-people-room:delete", targetType = "exam-people-rooms")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainExamPeopleInRoomApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
