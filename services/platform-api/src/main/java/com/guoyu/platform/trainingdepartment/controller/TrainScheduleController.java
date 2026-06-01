package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.service.TrainScheduleApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainScheduleConflictVO;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 培训排课管理接口
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainScheduleController {

  private final TrainScheduleApplicationService trainScheduleApplicationService;

  public TrainScheduleController(TrainScheduleApplicationService trainScheduleApplicationService) {
    this.trainScheduleApplicationService = trainScheduleApplicationService;
  }

  /** 检查排课冲突 */
  @GetMapping("/schedules/conflicts")
  @PreAuthorize("hasAuthority('train:class:view')")
  public ApiResponse<List<TrainScheduleConflictVO>> checkConflicts(@RequestParam Long classId) {
    return ApiResponse.ok(trainScheduleApplicationService.checkConflicts(classId));
  }

  /** 获取班级课表 */
  @GetMapping("/info-classes/{classId}/schedule")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<List<Map<String, Object>>> getSchedule(@PathVariable Long classId) {
    return ApiResponse.ok(trainScheduleApplicationService.getByClassId(classId));
  }

  /** 批量保存课表 */
  @PutMapping("/info-classes/{classId}/schedule")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "info-class:save-schedule", targetType = "train_schedule")
  @SuppressWarnings("unchecked")
  public ApiResponse<Map<String, Object>> saveSchedule(@PathVariable Long classId, @RequestBody Map<String, Object> body) {
    List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
    return ApiResponse.ok(trainScheduleApplicationService.saveSchedule(classId, items));
  }

  /** 导出课表 */
  @GetMapping("/info-classes/{classId}/schedule/export")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<Map<String, Object>> exportSchedule(@PathVariable Long classId) {
    return ApiResponse.ok(trainScheduleApplicationService.exportSchedule(classId));
  }
}
