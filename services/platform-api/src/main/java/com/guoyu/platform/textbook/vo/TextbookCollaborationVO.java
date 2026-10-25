package com.guoyu.platform.textbook.vo;

import java.util.List;

/** C2 工程编写管理 VO（方案 §6.1）：一次返回工程、合计校验、进度、完成条件与全部分配/节进度。 */
public record TextbookCollaborationVO(
    ProjectBrief project,
    OutlineSummaryVO outlineSummary,
    Progress progress,
    Completion completion,
    List<ChapterCollaboration> chapters) {

  public record ProjectBrief(String id, String status, String workTypeTitle, String direction,
                             Integer levelCode) {
  }

  public record Progress(
      int chapterCount,
      int assignedChapterCount,
      int sectionCount,
      int approvedSectionCount,
      long actualWords,
      long targetWords) {
  }

  public record Completion(boolean canComplete, List<Blocker> blockers) {
  }

  public record Blocker(String code, String chapterId, String sectionId, String message) {
  }

  public record ChapterCollaboration(
      String chapterId,
      String title,
      String sourceNo,
      Integer targetWords,
      TextbookAssignmentVO assignment,
      List<SectionProgress> sections) {
  }

  public record SectionProgress(
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
      TextbookReviewRecordVO latestReviewRecord) {
  }
}
