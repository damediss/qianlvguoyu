package com.guoyu.platform.competition.player.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageQuery;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.prepractice.dto.CompetitionPlayerPrePracticeAnswerSaveReq;
import com.guoyu.platform.competition.prepractice.service.CompetitionPlayerPrePracticeInboxApplicationService;
import com.guoyu.platform.competition.prepractice.service.CompetitionPlayerPrePracticeTaskApplicationService;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPlayerPrePracticeAnswerVO;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPlayerPrePracticeFeedbackVO;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPlayerPrePracticeTaskVO;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPlayerPrePracticeTaskDetailVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/competition/player/tasks")
public class CompetitionPlayerTaskController {

  private final CompetitionPlayerPrePracticeInboxApplicationService prePracticeService;
  private final CompetitionPlayerPrePracticeTaskApplicationService taskService;

  public CompetitionPlayerTaskController(
      CompetitionPlayerPrePracticeInboxApplicationService prePracticeService,
      CompetitionPlayerPrePracticeTaskApplicationService taskService) {
    this.prePracticeService = prePracticeService;
    this.taskService = taskService;
  }

  @GetMapping
  @PreAuthorize("hasRole('COMPETITION_PLAYER')")
  public ApiResponse<PageResponse<CompetitionPlayerPrePracticeTaskVO>> page(
      @Valid PageQuery query) {
    return ApiResponse.ok(prePracticeService.page(query));
  }

  @GetMapping("/{deliveryId}")
  @PreAuthorize("hasRole('COMPETITION_PLAYER')")
  public ApiResponse<CompetitionPlayerPrePracticeTaskDetailVO> detail(
      @PathVariable Long deliveryId) {
    return ApiResponse.ok(taskService.detail(deliveryId));
  }

  @GetMapping("/{deliveryId}/questions/{taskQuestionId}/feedback")
  @PreAuthorize("hasRole('COMPETITION_PLAYER')")
  public ApiResponse<CompetitionPlayerPrePracticeFeedbackVO> feedback(
      @PathVariable Long deliveryId,
      @PathVariable Long taskQuestionId) {
    return ApiResponse.ok(taskService.feedback(deliveryId, taskQuestionId));
  }

  @PutMapping("/{deliveryId}/questions/{taskQuestionId}/answer")
  @PreAuthorize("hasRole('COMPETITION_PLAYER')")
  @AuditLog(module = "competition-player", action = "pre-practice:save-answer",
      targetType = "competition_pre_practice_answer", includeArgs = false)
  public ApiResponse<CompetitionPlayerPrePracticeAnswerVO> saveAnswer(
      @PathVariable Long deliveryId,
      @PathVariable Long taskQuestionId,
      @Valid @RequestBody CompetitionPlayerPrePracticeAnswerSaveReq request) {
    return ApiResponse.ok(taskService.saveAnswer(deliveryId, taskQuestionId, request));
  }
}
