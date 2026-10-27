package com.guoyu.platform.competition.messaging.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.competition.entity.CompetitionNoticeEntity;
import com.guoyu.platform.competition.mapper.CompetitionNoticeMapper;
import com.guoyu.platform.competition.messaging.entity.CompetitionPlayerMessageEntity;
import com.guoyu.platform.competition.messaging.mapper.CompetitionPlayerMessageMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Idempotently restores published notices missing from one eligible player's inbox. */
@Service
@Slf4j
public class CompetitionNoticeInboxCompensationService {

  private static final String NOTICE_PUBLISHED = "notice_published";

  private final CompetitionNoticeMapper noticeMapper;
  private final CompetitionPlayerMessageMapper playerMessageMapper;

  public CompetitionNoticeInboxCompensationService(
      CompetitionNoticeMapper noticeMapper,
      CompetitionPlayerMessageMapper playerMessageMapper) {
    this.noticeMapper = noticeMapper;
    this.playerMessageMapper = playerMessageMapper;
  }

  @Transactional
  public int compensatePublishedNotices(Long tenantId, Long eventId, Long playerId) {
    if (tenantId == null || eventId == null || playerId == null) {
      return 0;
    }
    List<CompetitionNoticeEntity> missingNotices =
        noticeMapper.selectPublishedMissingFromPlayerInbox(tenantId, eventId, playerId);
    if (missingNotices == null || missingNotices.isEmpty()) {
      return 0;
    }

    LocalDateTime receivedAt = LocalDateTime.now();
    int compensated = 0;
    for (CompetitionNoticeEntity notice : missingNotices) {
      if (notice == null || notice.getId() == null) {
        continue;
      }
      compensated += deliverPublishedNotice(
          tenantId, eventId, notice.getId(), playerId, receivedAt);
    }
    if (compensated > 0) {
      log.info("Competition notice inbox compensated, tenantId={}, eventId={}, count={}",
          tenantId, eventId, compensated);
    }
    return compensated;
  }

  public int deliverPublishedNotice(
      Long tenantId, Long eventId, Long noticeId, Long playerId, LocalDateTime receivedAt) {
    return deliverPublishedNoticeWithResult(tenantId, eventId, noticeId, playerId, receivedAt).inserted()
        ? 1 : 0;
  }

  public DeliveryResult deliverPublishedNoticeWithResult(
      Long tenantId, Long eventId, Long noticeId, Long playerId, LocalDateTime receivedAt) {
    CompetitionPlayerMessageEntity message = new CompetitionPlayerMessageEntity();
    message.setId(IdWorker.getId());
    message.setTenantId(tenantId);
    message.setPlayerId(playerId);
    message.setEventId(eventId);
    message.setNoticeId(noticeId);
    message.setType(NOTICE_PUBLISHED);
    message.setReceivedAt(receivedAt);
    message.setCreatedAt(receivedAt);
    message.setUpdatedAt(receivedAt);
    message.setDeleted(0);
    int inserted = playerMessageMapper.insertIgnoreDuplicate(message);
    return new DeliveryResult(message.getId(), inserted > 0);
  }

  public record DeliveryResult(Long messageId, boolean inserted) {
  }
}
