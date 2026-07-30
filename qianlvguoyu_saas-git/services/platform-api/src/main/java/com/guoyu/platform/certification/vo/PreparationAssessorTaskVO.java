package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationAssessorTaskEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PreparationAssessorTaskVO(
    Long id,
    Long taskId,
    String taskName,
    Long groupId,
    String groupName,
    Long assessorId,
    String assessorName,
    Long leaderAssessorId,
    String leaderAssessorName,
    String subject,
    String title,
    Long assessmentFileId,
    String assessmentFileName,
    Integer durationMin,
    LocalDate dueDate,
    String status,
    String progressNote,
    BigDecimal totalScore,
    LocalDateTime submittedAt,
    LocalDateTime reviewedAt,
    String reviewNote,
    List<PreparationScoreVO> scores
) {
  public static PreparationAssessorTaskVO from(
      CertificationAssessorTaskEntity entity,
      String taskName,
      String groupName,
      String assessorName,
      String leaderAssessorName,
      List<PreparationScoreVO> scores
  ) {
    return new PreparationAssessorTaskVO(
        entity.getId(),
        entity.getTaskId(),
        taskName,
        entity.getGroupId(),
        groupName,
        entity.getAssessorId(),
        assessorName,
        entity.getLeaderAssessorId(),
        leaderAssessorName,
        entity.getSubject(),
        entity.getTitle(),
        entity.getAssessmentFileId(),
        entity.getAssessmentFileName(),
        entity.getDurationMin(),
        entity.getDueDate(),
        entity.getStatus(),
        entity.getProgressNote(),
        entity.getTotalScore(),
        entity.getSubmittedAt(),
        entity.getReviewedAt(),
        entity.getReviewNote(),
        scores
    );
  }
}
