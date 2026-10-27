package com.guoyu.platform.competition.messaging.model;

import java.time.LocalDateTime;

/** Client-facing summary that lets the homepage update immediately without a synchronized REST reload. */
public record CompetitionPlayerMessageAvailableEvent(
    String type,
    String messageId,
    String eventId,
    String noticeId,
    String title,
    String summary,
    String contentType,
    LocalDateTime publishedAt,
    int unreadDelta,
    String version) {

  public static final String TYPE = "competition.player.message.available.v1";
}
