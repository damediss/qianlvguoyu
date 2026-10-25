package com.guoyu.platform.textbook.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * W2 节正文 VO（方案 §6.3）。
 *
 * <p>空正文契约：授权通过但尚无正文记录时 contentExists=false、contentStatus=null、
 * contentMd=""、wordCount=0、version=0（不能用 404 表示）。
 * relations/canWrite/canReview/readOnlyReason 由读取授权与写能力分离计算（方案 §4.4）。
 */
public record TextbookSectionContentVO(
    String sectionId,
    String chapterId,
    String projectId,
    String sectionTitle,
    String chapterTitle,
    Integer targetWords,
    boolean contentExists,
    String contentMd,
    String contentStatus,
    Integer wordCount,
    Integer version,
    List<String> relations,
    boolean canWrite,
    boolean canReview,
    boolean readOnly,
    String readOnlyReason,
    TextbookCheckReportVO checkReport,
    List<AssetRef> assets,
    String submittedBy,
    String submittedByName,
    LocalDateTime submittedAt,
    Integer submitRevision,
    String currentSnapshotId,
    String reviewedByName,
    LocalDateTime reviewedAt,
    TextbookReviewRecordVO latestReviewRecord) {

  /** A1/A2 资产引用：编辑器内用 textbook-asset://{assetId}，不暴露 objectKey/URL。 */
  public record AssetRef(String assetId, String sourceUri) {
  }
}
