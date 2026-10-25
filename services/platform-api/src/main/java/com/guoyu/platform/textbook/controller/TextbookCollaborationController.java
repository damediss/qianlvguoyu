package com.guoyu.platform.textbook.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.textbook.dto.TextbookAssignmentBatchReq;
import com.guoyu.platform.textbook.dto.TextbookAssignmentCancelReq;
import com.guoyu.platform.textbook.dto.TextbookTaskQueryReq;
import com.guoyu.platform.textbook.service.TextbookAssignmentApplicationService;
import com.guoyu.platform.textbook.vo.TextbookAssignmentVO;
import com.guoyu.platform.textbook.vo.TextbookChapterWorkspaceVO;
import com.guoyu.platform.textbook.vo.TextbookCollaborationVO;
import com.guoyu.platform.textbook.vo.TextbookCollaboratorVO;
import com.guoyu.platform.textbook.vo.TextbookProjectVO;
import com.guoyu.platform.textbook.vo.TextbookTaskSummaryVO;
import com.guoyu.platform.textbook.vo.TextbookTaskVO;
import com.guoyu.platform.textbook.vo.TextbookCollaborationVO.Completion;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工程编写管理 + 我的任务（方案 §6.1 C1-C6、§6.2 T1-T4、§6.3 W1）。
 * 全部写操作 @AuditLog；权限三层模型的功能层在此（对象层在 TextbookAccessService）。
 */
@RestController
@RequestMapping("/textbook")
public class TextbookCollaborationController {

  private final TextbookAssignmentApplicationService assignmentService;
  private final com.guoyu.platform.textbook.service.TextbookProjectApplicationService projectService;

  public TextbookCollaborationController(
      TextbookAssignmentApplicationService assignmentService,
      com.guoyu.platform.textbook.service.TextbookProjectApplicationService projectService) {
    this.assignmentService = assignmentService;
    this.projectService = projectService;
  }

  /** C1 候选人分页：服务端按权限过滤 enabled 用户（不暴露手机号）。 */
  @GetMapping("/collaborators")
  @PreAuthorize("hasAuthority('textbook:project:manage')")
  public ApiResponse<PageResponse<TextbookCollaboratorVO>> collaborators(
      @RequestParam String role,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long pageSize) {
    return ApiResponse.ok(assignmentService.collaborators(role, keyword, page, pageSize));
  }

  /** C2 工程、合计校验、全部章分配与节进度一次返回。 */
  @GetMapping("/projects/{projectId}/collaboration")
  @PreAuthorize("hasAuthority('textbook:project:view')")
  public ApiResponse<TextbookCollaborationVO> collaboration(@PathVariable Long projectId) {
    return ApiResponse.ok(assignmentService.collaboration(projectId));
  }

  /** C3 批量创建/改派（单事务；1~50 章；全部成功才提交）。 */
  @PutMapping("/projects/{projectId}/assignments")
  @PreAuthorize("hasAuthority('textbook:project:manage')")
  @AuditLog(module = "textbook", action = "assignment:batch-assign",
      targetType = "textbook_chapter_assignment")
  public ApiResponse<List<TextbookAssignmentVO>> assignBatch(@PathVariable Long projectId,
      @Valid @RequestBody TextbookAssignmentBatchReq req) {
    return ApiResponse.ok(assignmentService.assignBatch(projectId, req));
  }

  /** C4 取消当前分配（assignmentVersion 必传）。 */
  @PostMapping("/chapters/{chapterId}/assignment/cancel")
  @PreAuthorize("hasAuthority('textbook:project:manage')")
  @AuditLog(module = "textbook", action = "assignment:cancel",
      targetType = "textbook_chapter_assignment")
  public ApiResponse<TextbookAssignmentVO> cancelAssignment(@PathVariable Long chapterId,
      @Valid @RequestBody TextbookAssignmentCancelReq req) {
    return ApiResponse.ok(assignmentService.cancelAssignment(chapterId,
        req.assignmentVersion()));
  }

  /** C5 完成编写：校验完成条件 → done；失败 409 data.completion 同 C2。 */
  @PostMapping("/projects/{projectId}/complete")
  @PreAuthorize("hasAuthority('textbook:project:manage')")
  @AuditLog(module = "textbook", action = "project:complete", targetType = "textbook_project")
  public ApiResponse<TextbookProjectVO> complete(@PathVariable Long projectId) {
    assignmentService.complete(projectId);
    return ApiResponse.ok(projectService.detail(projectId));
  }

  /** C6 重新打开：done → writing，不清正文状态（done 的唯一退出入口）。 */
  @PostMapping("/projects/{projectId}/reopen")
  @PreAuthorize("hasAuthority('textbook:project:manage')")
  @AuditLog(module = "textbook", action = "project:reopen", targetType = "textbook_project")
  public ApiResponse<TextbookProjectVO> reopen(@PathVariable Long projectId) {
    assignmentService.reopen(projectId);
    return ApiResponse.ok(projectService.detail(projectId));
  }

  /** T1 我的编写任务。 */
  @GetMapping("/my-writing-tasks")
  @PreAuthorize("hasAuthority('textbook:task:view')")
  public ApiResponse<PageResponse<TextbookTaskVO>> myWritingTasks(
      @Valid TextbookTaskQueryReq req) {
    return ApiResponse.ok(assignmentService.myTasks(false, req));
  }

  /** T2 我的审定任务（默认 submittedCount>0）。 */
  @GetMapping("/my-review-tasks")
  @PreAuthorize("hasAuthority('textbook:review:view')")
  public ApiResponse<PageResponse<TextbookTaskVO>> myReviewTasks(
      @Valid TextbookTaskQueryReq req) {
    return ApiResponse.ok(assignmentService.myTasks(true, req));
  }

  /** T3 编写统计卡：当前用户全量口径，不受筛选/分页影响（返回扁平 WritingTaskSummaryVO）。 */
  @GetMapping("/my-writing-tasks/summary")
  @PreAuthorize("hasAuthority('textbook:task:view')")
  public ApiResponse<TextbookTaskSummaryVO.WritingTaskSummaryVO> writingSummary() {
    return ApiResponse.ok(assignmentService.writingSummaryForCurrentUser());
  }

  /** T4 审定统计卡（返回扁平 ReviewTaskSummaryVO）。 */
  @GetMapping("/my-review-tasks/summary")
  @PreAuthorize("hasAuthority('textbook:review:view')")
  public ApiResponse<TextbookTaskSummaryVO.ReviewTaskSummaryVO> reviewSummary() {
    return ApiResponse.ok(assignmentService.reviewSummaryForCurrentUser());
  }

  /** W1 章工作台。 */
  @GetMapping("/chapters/{chapterId}/workspace")
  @PreAuthorize("hasAuthority('textbook:project:view') or hasAuthority('textbook:task:view') "
      + "or hasAuthority('textbook:review:view')")
  public ApiResponse<TextbookChapterWorkspaceVO> workspace(@PathVariable Long chapterId) {
    return ApiResponse.ok(assignmentService.workspace(chapterId));
  }
}
