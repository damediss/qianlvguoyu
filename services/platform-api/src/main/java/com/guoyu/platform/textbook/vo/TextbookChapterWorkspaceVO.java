package com.guoyu.platform.textbook.vo;

import java.util.List;

/** W1 章工作台 VO（方案 §6.3）。assignment 为完整 AssignmentVO。 */
public record TextbookChapterWorkspaceVO(
    String projectId,
    String projectStatus,
    String workTypeTitle,
    String direction,
    Integer levelCode,
    String chapterId,
    String chapterTitle,
    String chapterSourceNo,
    TextbookAssignmentVO assignment,
    List<String> relations,
    boolean canWrite,
    boolean canReview,
    String readOnlyReason,
    List<WorkspaceSectionVO> sections) {

  /** 工作台节列表项：含编写点（标准理论/技能点原文）。 */
  public record WorkspaceSectionVO(
      String sectionId,
      String title,
      String sourceNo,
      Integer sortNo,
      Integer targetWords,
      boolean contentExists,
      String contentStatus,
      Integer wordCount,
      Integer version,
      java.time.LocalDateTime submittedAt,
      String submittedByName,
      Integer submitRevision,
      String currentSnapshotId,
      TextbookReviewRecordVO latestReviewRecord,
      List<PointVO> points) {
  }

  public record PointVO(String pointId, String titleRaw, String dimension, Integer sortNo) {
  }
}
