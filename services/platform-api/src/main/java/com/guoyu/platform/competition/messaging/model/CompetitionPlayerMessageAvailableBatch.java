package com.guoyu.platform.competition.messaging.model;

import java.time.LocalDateTime;
import java.util.List;

/** Internal node broadcast with a short announcement summary and at most 500 recipient mappings. */
public record CompetitionPlayerMessageAvailableBatch(
    Long tenantId,
    Long eventId,
    Long noticeId,
    String title,
    String summary,
    String contentType,
    LocalDateTime publishedAt,
    List<Recipient> recipients) {

  public CompetitionPlayerMessageAvailableBatch {
    recipients = recipients == null ? List.of() : List.copyOf(recipients);
  }

  public record Recipient(Long playerId, Long messageId) {
  }
}
