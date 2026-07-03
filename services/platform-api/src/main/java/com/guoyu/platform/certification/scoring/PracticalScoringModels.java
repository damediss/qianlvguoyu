package com.guoyu.platform.certification.scoring;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PracticalScoringModels {

  private PracticalScoringModels() {
  }

  public record TemplateItem(
      @NotNull @Min(1) Integer itemNo,
      @NotBlank String itemName,
      @NotNull @DecimalMin("0.01") BigDecimal maxScore,
      @NotBlank String criterion,
      @NotNull @DecimalMin("0.00") BigDecimal maxDeduction,
      @NotNull @DecimalMin("0.01") BigDecimal deductionStep
  ) {
  }

  public record SaveConfigReq(
      @NotNull Long scopeId,
      // workTypeId 可空：scope 与 paper 都没有标准工种ID 时，后端 candidatesForConfig
      // 通过 workTypeName 匹配
      Long workTypeId,
      @NotNull Long paperId,
      @NotNull Long groupId
  ) {
  }

  public record LinkCandidateReq(
      Long scopeId,
      Long workTypeId,
      @NotNull Long paperId,
      @NotNull Long groupId
  ) {
  }

  public record SaveScoreItemReq(
      @NotNull @Min(1) Integer itemNo,
      @NotNull @DecimalMin("0.00") BigDecimal deductedScore,
      String remark
  ) {
  }

  public record SaveScoreReq(
      @NotNull @Min(0) Integer version,
      @NotEmpty List<@Valid SaveScoreItemReq> items,
      String note
  ) {
  }

  public record AdjustScoreReq(
      @NotNull @Min(0) Integer version,
      @NotEmpty List<@Valid SaveScoreItemReq> items,
      String note,
      @NotBlank String reason
  ) {
  }

  public record ReturnReq(@NotBlank String reason) {
  }

  public record ScopeOption(
      Long id,
      Long workTypeId,
      String workTypeName,
      String levelCode,
      String levelName
  ) {
  }

  public record GroupOption(
      Long id,
      String groupName,
      Long leaderAssessorId,
      int memberCount
  ) {
  }

  public record PaperQuestionOption(
      Long id,
      String title,
      BigDecimal score,
      boolean templateReady,
      List<TemplateItem> templateItems
  ) {
  }

  public record PaperOption(
      Long id,
      Long workTypeId,
      String level,
      String title,
      Integer totalPoint,
      List<PaperQuestionOption> questions
  ) {
  }

  public record ConfigView(
      Long id,
      Long taskId,
      Long scopeId,
      Long candidateId,
      Long workTypeId,
      String workTypeName,
      String levelCode,
      String levelName,
      LocalDate examDate,
      Long paperId,
      Long groupId,
      String status
  ) {
  }

  public record SetupView(
      Long taskId,
      LocalDate examDate,
      List<ScopeOption> scopes,
      List<GroupOption> groups,
      List<PaperOption> papers,
      List<ConfigView> configs
  ) {
  }

  public record GenerateView(int createdTasks, int existingTasks, int candidates, int questions) {
  }

  public record ImportPaperView(
      Long paperId,
      String title,
      int questions,
      int scoreItems
  ) {
  }

  public record ScoreItemView(
      Long id,
      Integer itemNo,
      String itemName,
      String criterion,
      BigDecimal maxScore,
      BigDecimal maxDeduction,
      BigDecimal deductionStep,
      BigDecimal deductedScore,
      BigDecimal awardedScore,
      String remark
  ) {
  }

  public record AssessorTaskListItem(
      Long id,
      Long taskId,
      Long candidateId,
      String candidateNo,
      String candidateName,
      String workTypeName,
      String levelName,
      Long paperQuestionId,
      String questionTitle,
      String status,
      BigDecimal totalScore,
      Integer version,
      Integer revision,
      LocalDateTime updatedAt,
      LocalDateTime submittedAt,
      boolean leader
  ) {
  }

  public record AssessorTaskDetail(
      AssessorTaskListItem task,
      String questionContent,
      String studentPrepare,
      String equipmentPrepare,
      List<ScoreItemView> scoreItems,
      String progressNote,
      String returnReason,
      StartSessionView startSession
  ) {
  }

  public record AssessorExamListItem(
      Long taskId,
      Long candidateId,
      Long paperId,
      String candidateNo,
      String candidateName,
      String workTypeName,
      String levelName,
      int submittedQuestions,
      int totalQuestions,
      Integer totalPoint,
      BigDecimal currentScore,
      String status,
      LocalDateTime updatedAt,
      StartSessionView startSession,
      String resultStatus,
      boolean reviewRequired
  ) {
  }

  public record AssessorExamDetail(
      AssessorExamListItem exam,
      List<AssessorTaskDetail> questions
  ) {
  }

  public record AssessorWorkbenchTask(
      Long taskId,
      String taskNo,
      String taskName,
      LocalDate examDate,
      int startedSessionCount
  ) {
  }

  public record AssessorWorkbenchContext(
      boolean leader,
      Long leaderAssessorId,
      String leaderAssessorName,
      Long activeTaskId,
      List<AssessorWorkbenchTask> switchableTasks
  ) {
  }

  public record SwitchWorkbenchTaskReq(@NotNull Long taskId) {
  }

  public record StartSessionView(
      Long id,
      Long taskId,
      Long candidateId,
      Long paperId,
      String status,
      boolean candidateConfirmed,
      int confirmedAssessors,
      int requiredAssessors,
      boolean currentAssessorConfirmed,
      Integer version,
      LocalDateTime startedAt,
      LocalDateTime completedAt,
      String summaryFileName
  ) {
  }

  public record CandidateProgress(
      Long candidateId,
      String candidateNo,
      String candidateName,
      int submittedTasks,
      int totalTasks,
      int completedQuestions,
      int totalQuestions,
      BigDecimal totalScore,
      String status,
      StartSessionView startSession
  ) {
  }

  public record TaskProgressItem(
      Long assessorTaskId,
      Long candidateId,
      String candidateName,
      Long paperQuestionId,
      String questionTitle,
      Long assessorId,
      String assessorName,
      String status,
      BigDecimal totalScore,
      LocalDateTime submittedAt,
      String questionResultStatus,
      BigDecimal scoreDeviation,
      boolean reviewRequired
  ) {
  }

  public record ProgressView(
      Long taskId,
      int submittedTasks,
      int totalTasks,
      int completedQuestions,
      int totalQuestions,
      List<CandidateProgress> candidates,
      List<TaskProgressItem> scoringTasks
  ) {
  }
}
