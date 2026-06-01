package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.service.TrainGraduationApplicationService;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 毕业管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainGraduationController {

  private final TrainGraduationApplicationService trainGraduationApplicationService;

  public TrainGraduationController(TrainGraduationApplicationService trainGraduationApplicationService) {
    this.trainGraduationApplicationService = trainGraduationApplicationService;
  }

  /** 可毕业班级列表 */
  @GetMapping("/info-classes/graduation-candidates")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<Map<String, Object>> graduationCandidates(
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "20") Integer pageSize) {
    return ApiResponse.ok(trainGraduationApplicationService.graduationCandidates(page, pageSize));
  }

  /** 班级可毕业学员列表 */
  @GetMapping("/info-classes/{classId}/graduation-students")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<Map<String, Object>> graduationStudents(@PathVariable Long classId) {
    return ApiResponse.ok(trainGraduationApplicationService.graduationStudents(classId));
  }

  /** 执行毕业 */
  @PostMapping("/info-classes/{classId}/graduate")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "info-class:graduate", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> graduate(@PathVariable Long classId) {
    return ApiResponse.ok(trainGraduationApplicationService.graduate(classId));
  }

  /** 发送毕业通知 */
  @PostMapping("/info-classes/{classId}/graduation-notify")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "info-class:graduation-notify", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> graduationNotify(@PathVariable Long classId) {
    return ApiResponse.ok(trainGraduationApplicationService.graduationNotify(classId));
  }
}
