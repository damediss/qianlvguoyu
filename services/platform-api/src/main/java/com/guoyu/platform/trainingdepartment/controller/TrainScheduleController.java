package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.security.TrainingAuthorityExpressions;
import com.guoyu.platform.trainingdepartment.service.TrainScheduleApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainScheduleConflictVO;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
  @PreAuthorize(TrainingAuthorityExpressions.SCHEDULE_CONFLICTS_VIEW)
  public ApiResponse<List<TrainScheduleConflictVO>> checkConflicts(@RequestParam Long classId) {
    return ApiResponse.ok(trainScheduleApplicationService.checkConflicts(classId));
  }

  /** 获取班级课表 */
  @GetMapping("/info-classes/{classId}/schedule")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<List<Map<String, Object>>> getSchedule(@PathVariable Long classId) {
    return ApiResponse.ok(trainScheduleApplicationService.getByClassId(classId));
  }

  /** 批量保存课表（跟班调课 / 课表配置；不回写开班 kcbiao_file） */
  @PutMapping("/info-classes/{classId}/schedule")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "info-class:save-schedule", targetType = "train_schedule")
  @SuppressWarnings("unchecked")
  public ApiResponse<Map<String, Object>> saveSchedule(@PathVariable Long classId, @RequestBody Map<String, Object> body) {
    List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
    return ApiResponse.ok(trainScheduleApplicationService.saveSchedule(classId, items));
  }

  /**
   * 从开班准备上传的课程表 Excel 导入结构化课次。
   * force=false：已有课次则跳过；force=true：覆盖现有 train_schedule。
   * confirmOverwriteManual=true：允许覆盖跟班已调课的课表。
   */
  @PostMapping("/info-classes/{classId}/schedule/import-from-kcbiao")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "info-class:import-schedule-kcbiao", targetType = "train_schedule")
  public ApiResponse<Map<String, Object>> importFromKcbiao(
      @PathVariable Long classId,
      @RequestParam(defaultValue = "false") boolean force,
      @RequestParam(defaultValue = "false") boolean confirmOverwriteManual) {
    return ApiResponse.ok(
        trainScheduleApplicationService.importFromKcbiao(classId, force, confirmOverwriteManual));
  }

  /** 课表双源状态（是否有开班文件、是否手工调过） */
  @GetMapping("/info-classes/{classId}/schedule/status")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<Map<String, Object>> scheduleStatus(@PathVariable Long classId) {
    return ApiResponse.ok(trainScheduleApplicationService.getScheduleStatus(classId));
  }

  /** 导出课表 */
  @GetMapping("/info-classes/{classId}/schedule/export")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<Map<String, Object>> exportSchedule(@PathVariable Long classId) {
    return ApiResponse.ok(trainScheduleApplicationService.exportSchedule(classId));
  }
}
