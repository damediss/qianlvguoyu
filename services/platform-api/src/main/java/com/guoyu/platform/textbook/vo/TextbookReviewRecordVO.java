package com.guoyu.platform.textbook.vo;

import java.time.LocalDateTime;

/** 审定时间线单条记录（W6 分页 / latestReviewRecord 内嵌）。 */
public record TextbookReviewRecordVO(
    String id,
    Integer submitRevision,
    String action,
    String fromStatus,
    String toStatus,
    String comment,
    String actorName,
    LocalDateTime createdAt) {
}
