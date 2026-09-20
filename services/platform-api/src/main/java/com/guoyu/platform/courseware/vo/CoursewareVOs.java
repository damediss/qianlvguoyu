package com.guoyu.platform.courseware.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.guoyu.platform.courseware.domain.StructureIssue;
import java.time.LocalDateTime;
import java.util.List;

/** courseware VO 集合;雪花 id 一律字符串下发(踩坑 #24) */
public final class CoursewareVOs {

  private CoursewareVOs() {
  }

  public record ProjectVO(
      @JsonSerialize(using = ToStringSerializer.class) Long id,
      String title,
      String audience,
      String contentDensity,
      String status,
      Integer totalPages,
      Integer readyPages,
      Integer failedPages,
      String error,
      LocalDateTime createdAt) {
  }

  public record OutlinePageVO(
      @JsonSerialize(using = ToStringSerializer.class) Long id,
      Integer chapterNo,
      String chapterTitle,
      Integer position,
      String title,
      String objective,
      List<String> keyPoints,
      String pageRole,
      String layoutId,
      List<String> sourceRefs,
      /** 固定页(cover/toc/review)不可删 */
      boolean fixed) {
  }

  public record SlideBriefVO(
      @JsonSerialize(using = ToStringSerializer.class) Long id,
      Integer position,
      String title,
      String pageRole,
      String status,
      String error) {
  }

  public record SlideDetailVO(
      @JsonSerialize(using = ToStringSerializer.class) Long id,
      Integer position,
      String title,
      String pageRole,
      String layoutId,
      JsonNode blocks,
      String speakerNotes,
      JsonNode issues,
      String status,
      String error,
      Integer revision) {
  }

  public record ProgressVO(
      String status,
      Integer totalPages,
      Integer readyPages,
      Integer failedPages,
      String error,
      List<SlideBriefVO> slides) {
  }

  public record IssueVO(String severity, String code, String slotId, String message) {

    public static IssueVO from(StructureIssue issue) {
      return new IssueVO(issue.severity(), issue.code(), issue.slotId(), issue.message());
    }
  }

  public record ExportResultVO(
      boolean blocked,
      List<IssueVO> issues,
      @JsonSerialize(using = ToStringSerializer.class) Long fileId,
      List<IssueVO> warnings) {
  }
}
