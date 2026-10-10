package com.guoyu.platform.competition.exam.realtime;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionFormalExamOutboxEntity;
import com.guoyu.platform.competition.exam.observability.FormalExamNodeIds;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamOutboxMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamRealtimeDispatchCursorMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class FormalExamOutboxDispatcher {

  private final CompetitionFormalExamOutboxMapper outboxMapper;
  private final CompetitionFormalExamRealtimeDispatchCursorMapper dispatchCursorMapper;
  private final StringRedisTemplate redis;
  private final FormalExamRealtimeHub hub;
  private final ObjectMapper objectMapper;
  private final FormalExamRealtimeProperties properties;
  private final Clock clock;
  private final TransactionTemplate transactionTemplate;
  private final AtomicLong publishingLeaseRecoveries = new AtomicLong();

  @org.springframework.beans.factory.annotation.Autowired
  public FormalExamOutboxDispatcher(
      CompetitionFormalExamOutboxMapper outboxMapper,
      CompetitionFormalExamRealtimeDispatchCursorMapper dispatchCursorMapper,
      @org.springframework.beans.factory.annotation.Autowired(required = false)
          StringRedisTemplate redis,
      FormalExamRealtimeHub hub,
      ObjectMapper objectMapper,
      FormalExamRealtimeProperties properties,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    this(outboxMapper, dispatchCursorMapper, redis, hub, objectMapper, properties, clock,
        new TransactionTemplate(transactionManager));
  }

  /** Test-friendly constructor. Production wiring uses the transaction-aware one above. */
  public FormalExamOutboxDispatcher(
      CompetitionFormalExamOutboxMapper outboxMapper,
      @org.springframework.beans.factory.annotation.Autowired(required = false)
          StringRedisTemplate redis,
      FormalExamRealtimeHub hub,
      ObjectMapper objectMapper,
      FormalExamRealtimeProperties properties,
      Clock clock) {
    this(outboxMapper, null, redis, hub, objectMapper, properties, clock,
        (TransactionTemplate) null);
  }

  private FormalExamOutboxDispatcher(
      CompetitionFormalExamOutboxMapper outboxMapper,
      CompetitionFormalExamRealtimeDispatchCursorMapper dispatchCursorMapper,
      StringRedisTemplate redis,
      FormalExamRealtimeHub hub,
      ObjectMapper objectMapper,
      FormalExamRealtimeProperties properties,
      Clock clock,
      TransactionTemplate transactionTemplate) {
    this.outboxMapper = outboxMapper;
    this.dispatchCursorMapper = dispatchCursorMapper;
    this.redis = redis;
    this.hub = hub;
    this.objectMapper = objectMapper;
    this.properties = properties;
    this.clock = clock;
    this.transactionTemplate = transactionTemplate;
  }

  public long publishingLeaseRecoveries() {
    return publishingLeaseRecoveries.get();
  }

  @Scheduled(fixedDelayString = "${platform.competition.formal-exam.realtime.dispatch-delay-millis:100}")
  public void dispatchPending() {
    if (!properties.isEnabled()) {
      return;
    }
    recoverExpiredPublishing();
    int remaining = Math.max(1, properties.getDispatchBatchSize());
    while (remaining > 0) {
      List<CompetitionFormalExamOutboxEntity> claimed = claimPendingBatch(remaining);
      if (claimed.isEmpty()) {
        break;
      }
      deliverClaimed(claimed);
      remaining -= claimed.size();
    }
  }

  public void publishOne(Long outboxId) {
    if (outboxId == null) {
      return;
    }
    CompetitionFormalExamOutboxEntity row = claimPendingHead(outboxId);
    if (row == null) {
      return;
    }
    deliverClaimed(List.of(row));
  }

  /**
   * Lock one eligible session head, then claim a contiguous prefix for that
   * session in the same short transaction. Once the head is publishing, the
   * exact-head query on another API node cannot skip forward into the batch.
   */
  private List<CompetitionFormalExamOutboxEntity> claimPendingBatch(int limit) {
    List<CompetitionFormalExamOutboxEntity> claimed = inTransaction(status -> {
      List<CompetitionFormalExamOutboxEntity> heads =
          outboxMapper.selectPendingHeadsForUpdate(1);
      if (heads == null || heads.isEmpty()) {
        return List.of();
      }
      CompetitionFormalExamOutboxEntity head = heads.get(0);
      if (head == null || head.getId() == null || head.getTenantId() == null
          || head.getExamSessionId() == null || head.getSeq() == null) {
        return List.of();
      }

      int batchLimit = Math.max(1, limit);
      List<CompetitionFormalExamOutboxEntity> candidates =
          outboxMapper.selectPendingBatchForSessionForUpdate(
              head.getTenantId(), head.getExamSessionId(), head.getSeq(), batchLimit);
      if (candidates == null || candidates.isEmpty()) {
        return List.of();
      }

      List<CompetitionFormalExamOutboxEntity> prefix = new ArrayList<>(candidates.size());
      long expectedSeq = head.getSeq();
      for (CompetitionFormalExamOutboxEntity row : candidates) {
        if (row == null || row.getId() == null || row.getSeq() == null
            || row.getSeq() != expectedSeq) {
          break;
        }
        prefix.add(row);
        expectedSeq += 1L;
      }
      if (prefix.isEmpty()) {
        return List.of();
      }

      List<Long> ids = prefix.stream().map(CompetitionFormalExamOutboxEntity::getId).toList();
      LocalDateTime now = LocalDateTime.now(clock);
      int changed = outboxMapper.markPublishingBatch(ids, now);
      if (changed != ids.size()) {
        throw new IllegalStateException(
            "Formal exam realtime outbox batch claim changed " + changed + " of " + ids.size());
      }
      for (CompetitionFormalExamOutboxEntity row : prefix) {
        row.setStatus("publishing");
        row.setAttempts((row.getAttempts() == null ? 0 : row.getAttempts()) + 1);
      }
      return prefix;
    });
    return claimed == null ? List.of() : claimed;
  }

  private CompetitionFormalExamOutboxEntity claimPendingHead(Long outboxId) {
    return inTransaction(status -> {
      CompetitionFormalExamOutboxEntity row =
          outboxMapper.selectPendingHeadByIdForUpdate(outboxId);
      if (row == null || !"pending".equals(row.getStatus())) {
        return null;
      }
      if (outboxMapper.markPublishing(row.getId(), LocalDateTime.now(clock)) != 1) {
        return null;
      }
      row.setStatus("publishing");
      row.setAttempts((row.getAttempts() == null ? 0 : row.getAttempts()) + 1);
      return row;
    });
  }

  private void deliverClaimed(List<CompetitionFormalExamOutboxEntity> rows) {
    if (rows == null || rows.isEmpty()) {
      return;
    }
    List<DeliveryResult> results = new ArrayList<>(rows.size());
    for (CompetitionFormalExamOutboxEntity row : rows) {
      DeliveryResult result;
      Long previousTenant = TenantContextHolder.getTenantId();
      try {
        TenantContextHolder.setTenantId(row.getTenantId());
        result = deliverToBus(row);
      } finally {
        if (previousTenant == null) {
          TenantContextHolder.clear();
        } else {
          TenantContextHolder.setTenantId(previousTenant);
        }
      }
      results.add(result);
      if (!result.success()) {
        break;
      }
    }
    completeBatch(rows, results);
  }

  private DeliveryResult deliverToBus(CompetitionFormalExamOutboxEntity row) {
    try {
      FormalExamRealtimeEnvelope envelope =
          objectMapper.readValue(row.getPayloadJson(), FormalExamRealtimeEnvelope.class);
      hub.deliver(envelope, row.getPayloadJson());
      if (redis != null) {
        String nodeId = FormalExamNodeIds.current(properties.getNodeId());
        redis.convertAndSend(
            FormalExamRealtimeTypes.CHANNEL,
            FormalExamRealtimeBusMessages.wrap(objectMapper, nodeId, row.getPayloadJson()));
      }
      return DeliveryResult.published();
    } catch (Exception ex) {
      String error = safeError(ex);
      int attempts = row.getAttempts() == null ? 1 : row.getAttempts();
      if (attempts >= Math.max(1, properties.getDispatchMaxAttempts())) {
        log.warn("Formal exam realtime outbox dropped, outboxId={}, type={}",
            row.getId(), row.getEventType());
        return DeliveryResult.dropped(error);
      }
      return DeliveryResult.retry(error);
    }
  }

  /**
   * Complete a claimed batch with at most one commit. A failure stops delivery
   * at that event; successful prefix rows are durable, while the failed and
   * untouched suffix are returned to pending in the same transaction.
   */
  private void completeBatch(
      List<CompetitionFormalExamOutboxEntity> rows,
      List<DeliveryResult> results) {
    if (rows.size() == 1 && results.size() == 1) {
      DeliveryResult result = results.get(0);
      LocalDateTime now = LocalDateTime.now(clock);
      if (result.success()) {
        finishSingle(rows.get(0), "published", now, null);
      } else if (result.dropped()) {
        finishSingle(rows.get(0), "dropped", now, result.error());
      } else {
        outboxMapper.markPendingRetry(rows.get(0).getId(), result.error(), now);
      }
      return;
    }

    inTransaction(transactionStatus -> {
      LocalDateTime now = LocalDateTime.now(clock);
      int completedCount = 0;
      int failureIndex = -1;
      DeliveryResult failure = null;
      for (int i = 0; i < results.size(); i++) {
        DeliveryResult result = results.get(i);
        if (!result.success()) {
          failureIndex = i;
          failure = result;
          break;
        }
        completedCount += 1;
      }

      if (completedCount > 0) {
        List<Long> publishedIds = rows.subList(0, completedCount).stream()
            .map(CompetitionFormalExamOutboxEntity::getId).toList();
        int changed = outboxMapper.markPublishedBatch(publishedIds, now);
        if (changed != publishedIds.size()) {
          throw new IllegalStateException(
              "Formal exam realtime outbox batch finish changed " + changed + " of "
                  + publishedIds.size());
        }
      }

      if (failureIndex >= 0 && failure != null) {
        CompetitionFormalExamOutboxEntity failedRow = rows.get(failureIndex);
        if (failure.dropped()) {
          if (outboxMapper.markFinished(
              failedRow.getId(), "dropped", now, failure.error()) != 1) {
            throw new IllegalStateException(
                "Formal exam realtime dropped finish failed for outbox " + failedRow.getId());
          }
          completedCount += 1;
        } else if (outboxMapper.markPendingRetry(failedRow.getId(), failure.error(), now) != 1) {
          throw new IllegalStateException(
              "Formal exam realtime retry reset failed for outbox " + failedRow.getId());
        }

        int suffixStart = failureIndex + 1;
        if (suffixStart < rows.size()) {
          List<Long> suffixIds = rows.subList(suffixStart, rows.size()).stream()
              .map(CompetitionFormalExamOutboxEntity::getId).toList();
          int reset = outboxMapper.markPendingRetryBatch(
              suffixIds, "batch-aborted-before-delivery", now);
          if (reset != suffixIds.size()) {
            throw new IllegalStateException(
                "Formal exam realtime suffix reset changed " + reset + " of " + suffixIds.size());
          }
        }
      }

      if (completedCount > 0) {
        advanceDispatchCursor(rows.get(0), rows.get(completedCount - 1), now);
      }
      return null;
    });
  }

  private void finishSingle(
      CompetitionFormalExamOutboxEntity row,
      String status,
      LocalDateTime publishedAt,
      String lastError) {
    inTransaction(transactionStatus -> {
      int changed = outboxMapper.markFinished(row.getId(), status, publishedAt, lastError);
      if (changed != 1) {
        return null;
      }
      advanceDispatchCursor(row, row, publishedAt);
      return null;
    });
  }

  private void advanceDispatchCursor(
      CompetitionFormalExamOutboxEntity first,
      CompetitionFormalExamOutboxEntity last,
      LocalDateTime now) {
    if (dispatchCursorMapper == null || first.getTenantId() == null
        || first.getExamSessionId() == null || first.getSeq() == null || last.getSeq() == null) {
      return;
    }
    dispatchCursorMapper.advanceOrCreate(
        IdWorker.getId(), first.getTenantId(), first.getExamSessionId(),
        first.getSeq() - 1L, last.getSeq(), now);
    Long actual = dispatchCursorMapper.selectDispatchSeq(
        first.getTenantId(), first.getExamSessionId());
    if (actual == null || actual.longValue() != last.getSeq()) {
      throw new IllegalStateException(
          "Formal exam realtime dispatch cursor did not advance for outbox " + last.getId());
    }
  }

  private void recoverExpiredPublishing() {
    int leaseSeconds = Math.max(5, properties.getPublishingLeaseSeconds());
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDateTime expiredBefore = now.minusSeconds(leaseSeconds);
    int recoveryBatch = Math.max(1, properties.getDispatchBatchSize());
    Integer recovered = inTransaction(status -> {
      List<Long> ids = outboxMapper.selectExpiredPublishingForUpdate(expiredBefore, recoveryBatch);
      if (ids == null || ids.isEmpty()) {
        return 0;
      }
      List<Long> validIds = ids.stream().filter(id -> id != null).toList();
      if (validIds.isEmpty()) {
        return 0;
      }
      int count = outboxMapper.resetExpiredPublishingBatch(validIds, now);
      if (count != validIds.size()) {
        throw new IllegalStateException(
            "Formal exam realtime lease recovery changed " + count + " of " + validIds.size());
      }
      return count;
    });
    if (recovered != null && recovered > 0) {
      publishingLeaseRecoveries.addAndGet(recovered);
      log.warn("Formal exam realtime publishing lease recovered, recovered={}, leaseSeconds={}",
          recovered, leaseSeconds);
    }
  }

  private <T> T inTransaction(TransactionCallback<T> callback) {
    if (transactionTemplate == null) {
      return callback.doInTransaction(null);
    }
    return transactionTemplate.execute(callback);
  }

  private static String safeError(Exception ex) {
    String message = ex.getMessage();
    if (!StringUtils.hasText(message)) {
      return ex.getClass().getSimpleName();
    }
    return message.length() <= 500 ? message : message.substring(0, 500);
  }

  private record DeliveryResult(boolean success, boolean dropped, String error) {
    private static DeliveryResult published() {
      return new DeliveryResult(true, false, null);
    }

    private static DeliveryResult retry(String error) {
      return new DeliveryResult(false, false, error);
    }

    private static DeliveryResult dropped(String error) {
      return new DeliveryResult(false, true, error);
    }
  }
}
