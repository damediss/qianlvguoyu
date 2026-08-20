package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.FollowIssueCreateReq;
import com.guoyu.platform.trainingdepartment.dto.FollowIssueTransitionReq;
import com.guoyu.platform.trainingdepartment.dto.FollowSupervisionCreateReq;
import com.guoyu.platform.trainingdepartment.dto.FollowSupervisionItemPatchReq;
import com.guoyu.platform.trainingdepartment.dto.FollowSupervisionSubmitReq;
import com.guoyu.platform.trainingdepartment.dto.FollowSurveyCreateReq;
import com.guoyu.platform.trainingdepartment.dto.FollowTeacherPaySaveReq;
import com.guoyu.platform.trainingdepartment.service.TrainFollowClassHourApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainFollowMetricsApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainFollowSupervisionApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainFollowTeacherDayApplicationService;
import com.guoyu.platform.trainingdepartment.vo.FollowClassHoursVO;
import com.guoyu.platform.trainingdepartment.vo.FollowIssueVO;
import com.guoyu.platform.trainingdepartment.vo.FollowSupervisionItemVO;
import com.guoyu.platform.trainingdepartment.vo.FollowSupervisionVO;
import com.guoyu.platform.trainingdepartment.vo.FollowSurveyResponseVO;
import com.guoyu.platform.trainingdepartment.vo.FollowTeacherDaysVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端跟班督导、整改、满意度与质量指标。
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainFollowQualityAdminController {

  private final TrainFollowSupervisionApplicationService supervisionService;
  private final TrainFollowMetricsApplicationService metricsService;
  private final TrainFollowClassHourApplicationService classHourService;
  private final TrainFollowTeacherDayApplicationService teacherDayService;

  public TrainFollowQualityAdminController(
      TrainFollowSupervisionApplicationService supervisionService,
      TrainFollowMetricsApplicationService metricsService,
      TrainFollowClassHourApplicationService classHourService,
      TrainFollowTeacherDayApplicationService teacherDayService) {
    this.supervisionService = supervisionService;
    this.metricsService = metricsService;
    this.classHourService = classHourService;
    this.teacherDayService = teacherDayService;
  }

  @GetMapping("/info-classes/{classId}/follow/supervisions")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<List<FollowSupervisionVO>> listSupervisions(
      @PathVariable Long classId,
      @RequestParam(required = false) String bizDate) {
    return ApiResponse.ok(supervisionService.listSupervisions(classId, bizDate));
  }

  @GetMapping("/follow-supervisions/{id}")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<FollowSupervisionVO> getSupervision(@PathVariable Long id) {
    return ApiResponse.ok(supervisionService.getSupervision(id));
  }

  @PostMapping("/info-classes/{classId}/follow/supervisions")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-supervision:create", targetType = "train_follow_supervision")
  public ApiResponse<FollowSupervisionVO> createSupervision(
      @PathVariable Long classId, @Valid @RequestBody FollowSupervisionCreateReq body) {
    return ApiResponse.ok(supervisionService.create(classId, body));
  }

  @PatchMapping("/follow-supervision-items/{itemId}")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-supervision-item:patch", targetType = "train_follow_supervision_item")
  public ApiResponse<FollowSupervisionItemVO> patchItem(
      @PathVariable Long itemId, @RequestBody FollowSupervisionItemPatchReq body) {
    return ApiResponse.ok(supervisionService.patchItem(itemId, body));
  }

  @PostMapping("/follow-supervisions/{id}/submit")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-supervision:submit", targetType = "train_follow_supervision")
  public ApiResponse<FollowSupervisionVO> submitSupervision(
      @PathVariable Long id, @RequestBody FollowSupervisionSubmitReq body) {
    return ApiResponse.ok(supervisionService.submit(id, body));
  }

  @PostMapping("/follow-supervisions/{id}/close")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-supervision:close", targetType = "train_follow_supervision")
  public ApiResponse<FollowSupervisionVO> closeSupervision(@PathVariable Long id) {
    return ApiResponse.ok(supervisionService.close(id));
  }

  @GetMapping("/info-classes/{classId}/follow/issues")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<List<FollowIssueVO>> listIssues(
      @PathVariable Long classId,
      @RequestParam(required = false) String status) {
    return ApiResponse.ok(supervisionService.listIssues(classId, status));
  }

  @PostMapping("/info-classes/{classId}/follow/issues")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-issue:create", targetType = "train_follow_issue")
  public ApiResponse<FollowIssueVO> createIssue(
      @PathVariable Long classId, @Valid @RequestBody FollowIssueCreateReq body) {
    return ApiResponse.ok(supervisionService.createIssue(classId, body));
  }

  @PostMapping("/follow-issues/{id}/transition")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-issue:transition", targetType = "train_follow_issue")
  public ApiResponse<FollowIssueVO> transitionIssue(
      @PathVariable Long id, @RequestBody FollowIssueTransitionReq body) {
    return ApiResponse.ok(supervisionService.transitionIssue(id, body));
  }

  @GetMapping("/info-classes/{classId}/follow/surveys")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<List<FollowSurveyResponseVO>> listSurveys(@PathVariable Long classId) {
    return ApiResponse.ok(supervisionService.listSurveys(classId));
  }

  @PostMapping("/info-classes/{classId}/follow/surveys")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-survey:create", targetType = "train_follow_survey_response")
  public ApiResponse<FollowSurveyResponseVO> createSurvey(
      @PathVariable Long classId, @Valid @RequestBody FollowSurveyCreateReq body) {
    return ApiResponse.ok(supervisionService.createSurvey(classId, body));
  }

  @GetMapping("/info-classes/{classId}/follow/quality-metrics")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<Map<String, Object>> qualityMetrics(@PathVariable Long classId) {
    return ApiResponse.ok(metricsService.classQualityMetrics(classId));
  }

  @GetMapping("/info-classes/{classId}/follow/class-hours")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<FollowClassHoursVO> classHours(@PathVariable Long classId) {
    return ApiResponse.ok(classHourService.classHours(classId));
  }

  @GetMapping("/info-classes/{classId}/follow/teacher-days")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<FollowTeacherDaysVO> teacherDays(@PathVariable Long classId) {
    return ApiResponse.ok(teacherDayService.teacherDays(classId));
  }

  @PutMapping("/info-classes/{classId}/follow/teacher-pay")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "follow-teacher-pay:save", targetType = "train_info_class")
  public ApiResponse<FollowTeacherDaysVO> saveTeacherPay(
      @PathVariable Long classId, @Valid @RequestBody FollowTeacherPaySaveReq req) {
    return ApiResponse.ok(teacherDayService.savePay(classId, req));
  }

  @GetMapping("/info-classes/{classId}/follow/teacher-days/export")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  public ResponseEntity<byte[]> exportTeacherDays(@PathVariable Long classId) {
    return teacherDayService.exportExcel(classId);
  }
}
