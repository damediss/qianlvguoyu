package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.trainingdepartment.dto.FollowBankDrawReq;
import com.guoyu.platform.trainingdepartment.dto.FollowTaskSaveReq;
import com.guoyu.platform.trainingdepartment.dto.FollowTaskSubmitReq;
import com.guoyu.platform.trainingdepartment.dto.ScheduleChangeSubmitReq;
import com.guoyu.platform.trainingdepartment.service.TrainClassRoleApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainFollowTaskApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainScheduleApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainScheduleChangeRequestApplicationService;
import com.guoyu.platform.trainingdepartment.vo.FollowStudentTaskVO;
import com.guoyu.platform.trainingdepartment.vo.FollowTaskItemVO;
import com.guoyu.platform.trainingdepartment.vo.FollowTaskResultVO;
import com.guoyu.platform.trainingdepartment.vo.FollowTaskStatsVO;
import com.guoyu.platform.trainingdepartment.vo.FollowTaskVO;
import com.guoyu.platform.trainingdepartment.vo.TrainMyClassVO;
import com.guoyu.platform.trainingdepartment.vo.TrainScheduleChangeRequestVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
 * App 跟班入口 API —— A1 / 课表 S1–S2 / 作业 M3-b。
 *
 * <p>鉴权：Bearer itoken → {@code AppTokenAuthFilter} → {@link PlatformUserPrincipal}；
 * 不加 {@code @PreAuthorize}（App authorities 为空）。
 */
@Validated
@RestController
@RequestMapping("/training/follow")
public class TrainFollowAppController {

  private final TrainClassRoleApplicationService roleService;
  private final TrainScheduleApplicationService scheduleService;
  private final TrainScheduleChangeRequestApplicationService changeRequestService;
  private final TrainFollowTaskApplicationService taskService;

  public TrainFollowAppController(
      TrainClassRoleApplicationService roleService,
      TrainScheduleApplicationService scheduleService,
      TrainScheduleChangeRequestApplicationService changeRequestService,
      TrainFollowTaskApplicationService taskService) {
    this.roleService = roleService;
    this.scheduleService = scheduleService;
    this.changeRequestService = changeRequestService;
    this.taskService = taskService;
  }

  @GetMapping("/my-classes")
  public ApiResponse<List<TrainMyClassVO>> myClasses() {
    return ApiResponse.ok(roleService.listMyClasses(requirePlatformUserId()));
  }

  @GetMapping("/classes/{classId}/role-summary")
  public ApiResponse<Map<String, Object>> roleSummary(@PathVariable Long classId) {
    return ApiResponse.ok(roleService.roleSummaryForMember(requirePlatformUserId(), classId));
  }

  @GetMapping("/classes/{classId}/schedule")
  public ApiResponse<List<Map<String, Object>>> schedule(@PathVariable Long classId) {
    roleService.assertClassMember(requirePlatformUserId(), classId);
    return ApiResponse.ok(scheduleService.getByClassIdForApp(classId));
  }

  @PostMapping("/classes/{classId}/schedule-change-requests")
  public ApiResponse<TrainScheduleChangeRequestVO> submitChange(
      @PathVariable Long classId,
      @Valid @RequestBody ScheduleChangeSubmitReq body) {
    return ApiResponse.ok(changeRequestService.submit(requirePlatformUserId(), classId, body));
  }

  @GetMapping("/classes/{classId}/schedule-change-requests/mine")
  public ApiResponse<List<TrainScheduleChangeRequestVO>> myChanges(@PathVariable Long classId) {
    return ApiResponse.ok(changeRequestService.listMine(requirePlatformUserId(), classId));
  }

  @PostMapping("/schedule-change-requests/{id}/cancel")
  public ApiResponse<TrainScheduleChangeRequestVO> cancelChange(@PathVariable Long id) {
    return ApiResponse.ok(changeRequestService.cancel(requirePlatformUserId(), id));
  }

  // ---------- 跟班作业（老师） ----------

  @GetMapping("/classes/{classId}/tasks")
  public ApiResponse<List<FollowTaskVO>> classTasks(@PathVariable Long classId) {
    return ApiResponse.ok(taskService.listByClassForTeacher(requirePlatformUserId(), classId));
  }

  /** 题库选题：本班练习范围客观题 */
  @GetMapping("/classes/{classId}/bank-questions")
  public ApiResponse<Map<String, Object>> bankQuestions(
      @PathVariable Long classId,
      @RequestParam(required = false) Integer courseId,
      @RequestParam(required = false) Integer knowledgeId,
      @RequestParam(required = false) String questionType,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.ok(taskService.searchBankQuestions(
        requirePlatformUserId(), classId, courseId, knowledgeId, questionType, keyword, page, pageSize));
  }

  /** 题库选题元信息（可选科目列表） */
  @GetMapping("/classes/{classId}/bank-scope")
  public ApiResponse<Map<String, Object>> bankScope(@PathVariable Long classId) {
    return ApiResponse.ok(taskService.bankScopeMeta(requirePlatformUserId(), classId));
  }

  /** 题库选题：按科目返回知识点分类树（受练习范围白名单裁剪） */
  @GetMapping("/classes/{classId}/bank-knowledge-tree")
  public ApiResponse<Map<String, Object>> bankKnowledgeTree(
      @PathVariable Long classId,
      @RequestParam Integer courseId) {
    return ApiResponse.ok(taskService.bankKnowledgeTree(requirePlatformUserId(), classId, courseId));
  }

  /** 题库智能抽题：按知识点范围配置各题型数量随机抽取 */
  @PostMapping("/classes/{classId}/bank-draw")
  public ApiResponse<Map<String, Object>> bankDraw(
      @PathVariable Long classId,
      @Valid @RequestBody FollowBankDrawReq body) {
    return ApiResponse.ok(taskService.drawBankQuestions(requirePlatformUserId(), classId, body));
  }

  @PostMapping("/tasks")
  public ApiResponse<FollowTaskVO> createTask(@Valid @RequestBody FollowTaskSaveReq body) {
    return ApiResponse.ok(taskService.createDraft(requirePlatformUserId(), body));
  }

  @PutMapping("/tasks/{id}")
  public ApiResponse<FollowTaskVO> updateTask(
      @PathVariable Long id, @Valid @RequestBody FollowTaskSaveReq body) {
    return ApiResponse.ok(taskService.updateDraft(requirePlatformUserId(), id, body));
  }

  @PostMapping("/tasks/{id}/publish")
  public ApiResponse<FollowTaskVO> publishTask(@PathVariable Long id) {
    return ApiResponse.ok(taskService.publish(requirePlatformUserId(), id));
  }

  @PostMapping("/tasks/{id}/close")
  public ApiResponse<FollowTaskVO> closeTask(@PathVariable Long id) {
    return ApiResponse.ok(taskService.close(requirePlatformUserId(), id));
  }

  @GetMapping("/tasks/{id}/items")
  public ApiResponse<List<FollowTaskItemVO>> taskItems(@PathVariable Long id) {
    return ApiResponse.ok(taskService.listItemsForTeacher(requirePlatformUserId(), id));
  }

  @GetMapping("/tasks/{id}/stats")
  public ApiResponse<FollowTaskStatsVO> taskStats(@PathVariable Long id) {
    return ApiResponse.ok(taskService.getTaskStatsForTeacher(requirePlatformUserId(), id));
  }

  @GetMapping("/tasks/{id}/items/{itemId}/answers")
  public ApiResponse<FollowTaskResultVO> teacherItemAnswers(
      @PathVariable Long id, @PathVariable Long itemId) {
    return ApiResponse.ok(taskService.getItemAnswersForTeacher(requirePlatformUserId(), id, itemId));
  }

  // ---------- 跟班作业（学员） ----------

  @GetMapping("/student/tasks")
  public ApiResponse<List<FollowStudentTaskVO>> studentTasks(@RequestParam Long classId) {
    return ApiResponse.ok(taskService.listStudentTasks(requirePlatformUserId(), classId));
  }

  @GetMapping("/tasks/{id}/paper")
  public ApiResponse<Map<String, Object>> paper(@PathVariable Long id) {
    return ApiResponse.ok(taskService.getPaper(requirePlatformUserId(), id));
  }

  @PostMapping("/tasks/{id}/start")
  public ApiResponse<Map<String, Object>> startTask(@PathVariable Long id) {
    return ApiResponse.ok(taskService.startTask(requirePlatformUserId(), id));
  }

  @PostMapping("/tasks/{id}/submit")
  public ApiResponse<FollowTaskResultVO> submit(
      @PathVariable Long id, @Valid @RequestBody FollowTaskSubmitReq body) {
    return ApiResponse.ok(taskService.submit(requirePlatformUserId(), id, body));
  }

  @GetMapping("/tasks/{id}/result")
  public ApiResponse<FollowTaskResultVO> result(@PathVariable Long id) {
    return ApiResponse.ok(taskService.getResult(requirePlatformUserId(), id));
  }

  private static long requirePlatformUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getPrincipal() instanceof PlatformUserPrincipal principal
        && principal.getUserId() != null
        && principal.getUserId() > 0) {
      return principal.getUserId();
    }
    throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或登录已过期");
  }
}
