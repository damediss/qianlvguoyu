package com.guoyu.platform.competition.messaging.model;

import java.time.LocalDateTime;

/** Message payload intentionally contains identifiers only and never announcement content or PII. */
public record CompetitionNoticePublishedEvent(
    Long tenantId,
    Long eventId,
    Long noticeId,
    LocalDateTime publishedAt) {
}
