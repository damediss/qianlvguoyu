package com.guoyu.platform.textbook.vo;

/** W7 快照列表 VO：不含大正文。 */
public record TextbookVersionVO(
    String id,
    Integer snapshotNo,
    String snapshotType,
    Integer submitRevision,
    Integer wordCount,
    String createdByName,
    java.time.LocalDateTime createdAt) {
}
