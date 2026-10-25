package com.guoyu.platform.textbook.vo;

/** W8 单个快照完整内容（方案 §6.3）。 */
public record TextbookVersionDetailVO(
    String id,
    String sectionId,
    Integer snapshotNo,
    String snapshotType,
    Integer submitRevision,
    Integer wordCount,
    String contentMd,
    String createdByName,
    java.time.LocalDateTime createdAt) {
}
