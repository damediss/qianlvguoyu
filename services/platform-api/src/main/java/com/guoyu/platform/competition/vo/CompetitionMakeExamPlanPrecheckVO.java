package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;
import java.util.List;

public record CompetitionMakeExamPlanPrecheckVO(
    Boolean ready,
    Integer candidateCount,
    List<TopicItem> topics,
    List<DetailItem> details,
    List<String> errors,
    List<TheoryAllocationItem> theoryAllocations,
    Integer unboundQuestionCount,
    List<String> warnings,
    List<PracticalAllocationItem> practicalAllocations,
    Integer practicalQuestionCount,
    Integer practicalReferenceDurationMinutes
) {
  /** Source-compatible constructor retained for callers that only need the legacy summary. */
  public CompetitionMakeExamPlanPrecheckVO(
      Boolean ready,
      Integer candidateCount,
      List<TopicItem> topics,
      List<DetailItem> details,
      List<String> errors) {
    this(ready, candidateCount, topics, details, errors, List.of(), 0, List.of(), List.of(), 0, 0);
  }

  /** Source-compatible constructor retained for the theory precheck callers. */
  public CompetitionMakeExamPlanPrecheckVO(
      Boolean ready,
      Integer candidateCount,
      List<TopicItem> topics,
      List<DetailItem> details,
      List<String> errors,
      List<TheoryAllocationItem> theoryAllocations,
      Integer unboundQuestionCount,
      List<String> warnings) {
    this(ready, candidateCount, topics, details, errors, theoryAllocations, unboundQuestionCount,
        warnings, List.of(), 0, 0);
  }

  public record TopicItem(
      String blueprintNodeId,
      String path,
      BigDecimal weight,
      Integer sortOrder,
      Integer required,
      Integer available,
      Integer shortage,
      List<DetailAllocationItem> allocations) {

    public TopicItem(String blueprintNodeId, String path, Integer required, Integer available, Integer shortage) {
      this(blueprintNodeId, path, null, null, required, available, shortage, List.of());
    }
  }

  public record DetailItem(
      Integer ruleIndex,
      Integer sortOrder,
      String questionType,
      Integer required,
      Integer available,
      Integer shortage) {

    public DetailItem(String questionType, Integer required, Integer available, Integer shortage) {
      this(null, null, questionType, required, available, shortage);
    }
  }

  /** Read-only matrix row used by the theory plan editor. */
  public record TheoryAllocationItem(
      String blueprintNodeId,
      String path,
      BigDecimal weight,
      Integer sortOrder,
      Integer questionCount,
      Integer available,
      Integer assigned,
      Integer shortage,
      List<DetailAllocationItem> details) {
  }

  /** One rule-bucket x level-3 range cell in the automatic allocation matrix. */
  public record DetailAllocationItem(
      Integer ruleIndex,
      Integer sortOrder,
      String questionType,
      BigDecimal point,
      Integer difficultyStart,
      Integer difficultyEnd,
      Integer required,
      Integer available,
      Integer assigned,
      Integer shortage) {
  }

  /** Read-only practical structure result shared by precheck and paper generation. */
  public record PracticalAllocationItem(
      String selectionGroup,
      String selectionMode,
      String blueprintNodeId,
      String path,
      BigDecimal scoreWeight,
      BigDecimal targetScore,
      Integer candidateQuestionCount,
      List<PracticalCandidateNode> candidateNodes,
      String selectedQuestionId,
      String selectedAssessmentPointId,
      BigDecimal sourceTotalScore,
      BigDecimal scoreScale,
      Integer referenceDurationMinutes) {
  }

  public record PracticalCandidateNode(
      String blueprintNodeId,
      String path,
      BigDecimal scoreWeight,
      List<String> assessmentPointIds,
      Integer candidateQuestionCount,
      List<String> candidateQuestionIds,
      Boolean selected) {
  }
}
