package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainExamListApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainExamListVO;
import com.guoyu.platform.trainingdepartment.vo.TrainExamPeopleListVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/training")
public class TrainExamListController {

  private final TrainExamListApplicationService trainExamListApplicationService;

  public TrainExamListController(TrainExamListApplicationService trainExamListApplicationService) {
    this.trainExamListApplicationService = trainExamListApplicationService;
  }

  @GetMapping("/exam-lists")
  @PreAuthorize("hasAuthority('train:exam-list:view')")
  public ApiResponse<PageResponse<TrainExamListVO>> page(@Valid @ModelAttribute TrainExamListQueryReq req) {
    return ApiResponse.ok(trainExamListApplicationService.page(req));
  }

  @GetMapping("/exam-lists/{id:\\d+}")
  @PreAuthorize("hasAuthority('train:exam-list:view')")
  public ApiResponse<TrainExamListVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainExamListApplicationService.getById(id));
  }

  @PostMapping("/exam-lists")
  @PreAuthorize("hasAuthority('train:exam-list:create')")
  @AuditLog(module = "training", action = "exam-list:create", targetType = "train_exam_list")
  public ApiResponse<TrainExamListVO> create(@Valid @RequestBody TrainExamListCreateReq req) {
    return ApiResponse.ok(trainExamListApplicationService.create(req));
  }

  @PutMapping("/exam-lists/{id}")
  @PreAuthorize("hasAuthority('train:exam-list:update')")
  @AuditLog(module = "training", action = "exam-list:update", targetType = "train_exam_list")
  public ApiResponse<TrainExamListVO> update(@PathVariable Long id, @Valid @RequestBody TrainExamListUpdateReq req) {
    return ApiResponse.ok(trainExamListApplicationService.update(id, req));
  }

  @DeleteMapping("/exam-lists/{id}")
  @PreAuthorize("hasAuthority('train:exam-list:delete')")
  @AuditLog(module = "training", action = "exam-list:delete", targetType = "train_exam_list")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainExamListApplicationService.delete(id);
    return ApiResponse.ok();
  }

  // ======== 考生成绩管理 ========

  @GetMapping("/exam-lists/{examId}/people-grades")
  @PreAuthorize("hasAuthority('train:exam-list:view')")
  public ApiResponse<PageResponse<TrainExamPeopleListVO>> getPeopleGradeList(
      @PathVariable Long examId,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer pageSize) {
    return ApiResponse.ok(trainExamListApplicationService.getPeopleGradeList(examId, keyword, page, pageSize));
  }

  @PutMapping("/exam-lists/people-grades/{id}")
  @PreAuthorize("hasAuthority('train:exam-list:update')")
  @AuditLog(module = "training", action = "exam-list:grade-update", targetType = "train_exam_people_list")
  public ApiResponse<Void> updatePeopleGrade(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    trainExamListApplicationService.updatePeopleGrade(id,
        body.get("inWhiteList") != null ? ((Number) body.get("inWhiteList")).intValue() : null,
        body.get("normalScore") != null ? new java.math.BigDecimal(body.get("normalScore").toString()) : null,
        body.get("score") != null ? new java.math.BigDecimal(body.get("score").toString()) : null,
        body.get("totalScore") != null ? new java.math.BigDecimal(body.get("totalScore").toString()) : null);
    return ApiResponse.ok();
  }

  // ======== 考生导入/批量删除 ========

  @PostMapping("/exam-lists/{examId}/import-people")
  @PreAuthorize("hasAuthority('train:exam-list:create')")
  @AuditLog(module = "training", action = "exam-list:import-people", targetType = "train_exam_people_list")
  public ApiResponse<Map<String, Object>> importPeopleByIdCard(
      @PathVariable Long examId, @RequestBody Map<String, String> body) {
    int count = trainExamListApplicationService.importPeopleByIdCard(examId, body.get("idCardNumbers"));
    return ApiResponse.ok(Map.of("count", count));
  }

  @PostMapping("/exam-lists/people/batch-delete")
  @PreAuthorize("hasAuthority('train:exam-list:delete')")
  @AuditLog(module = "training", action = "exam-list:batch-delete-people", targetType = "train_exam_people_list")
  public ApiResponse<Void> batchDeletePeople(@RequestBody Map<String, List<Long>> body) {
    trainExamListApplicationService.batchDeletePeople(body.get("ids"));
    return ApiResponse.ok();
  }

  @PostMapping("/exam-lists/{examId}/import-from-train/{trainId}")
  @PreAuthorize("hasAuthority('train:exam-list:create')")
  @AuditLog(module = "training", action = "exam-list:import-from-train", targetType = "train_exam_people_list")
  public ApiResponse<Map<String, Object>> importFromTrain(
      @PathVariable Long examId, @PathVariable Long trainId) {
    int count = trainExamListApplicationService.importPeopleFromTrain(examId, trainId);
    return ApiResponse.ok(Map.of("count", count));
  }
}
