package com.guoyu.platform.competition.exam.realtime;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalExamOutboxEntity;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamOutboxMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamRealtimeCursorMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class FormalExamRealtimePublisher {

  private final CompetitionFormalExamOutboxMapper outboxMapper;
  private final CompetitionFormalExamRealtimeCursorMapper cursorMapper;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final TransactionTemplate transactionTemplate;

  public FormalExamRealtimePublisher(
      CompetitionFormalExamOutboxMapper outboxMapper,
      CompetitionFormalExamRealtimeCursorMapper cursorMapper,
      ObjectMapper objectMapper,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    this.outboxMapper = outboxMapper;
    this.cursorMapper = cursorMapper;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.transactionTemplate.setPropagationBehavior(Propagation.REQUIRED.value());
  }

  public void candidateStatus(
      Long tenantId,
      Long examSessionId,
      Long candidateExamId,
      Long accessSessionId,
      Integer generation,
      String status) {
    candidateStatus(tenantId, examSessionId, candidateExamId, accessSessionId, generation, null, status);
  }

  public void candidateStatus(
      Long tenantId,
      Long examSessionId,
      Long candidateExamId,
      Long accessSessionId,
      Integer generation,
      String previousStatus,
      String status) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("status", status);
    if (StringUtils.hasText(previousStatus)) {
      payload.put("previousStatus", previousStatus);
    }
    String target = accessSessionId == null
        ? FormalExamRealtimeTypes.TARGET_SESSION
        : FormalExamRealtimeTypes.TARGET_ACCESS;
    enqueue(tenantId, FormalExamRealtimeTypes.CANDIDATE_STATUS_CHANGED,
        candidateExamId, examSessionId, candidateExamId, accessSessionId, generation,
        target, payload, null);
  }

  public void sessionStatus(Long tenantId, Long examSessionId, String status) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("status", status);
    enqueue(tenantId, FormalExamRealtimeTypes.SESSION_STATUS_CHANGED,
        examSessionId, examSessionId, null, null, null,
        FormalExamRealtimeTypes.TARGET_SESSION, payload, null);
  }

  public void snapshotReload(Long tenantId, Long examSessionId, String reason) {
    Map<String, Object> payload = new LinkedHashMap<>();
    if (StringUtils.hasText(reason)) {
      payload.put("reason", reason);
    }
    enqueue(tenantId, FormalExamRealtimeTypes.SNAPSHOT_RELOAD_REQUIRED,
        examSessionId, examSessionId, null, null, null,
        FormalExamRealtimeTypes.TARGET_SESSION, payload, null);
  }

  public void kicked(
      Long tenantId,
      Long examSessionId,
      Long candidateExamId,
      Long accessSessionId,
      Integer generation,
      Long commandId,
      Instant expiresAt) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("reason", "kicked");
    enqueue(tenantId, FormalExamRealtimeTypes.ACCESS_SESSION_KICKED,
        accessSessionId, examSessionId, candidateExamId, accessSessionId, generation,
        FormalExamRealtimeTypes.TARGET_ACCESS, payload, commandId, expiresAt);
  }

  public void forceSubmitCompleted(
      Long tenantId,
      Long examSessionId,
      Long candidateExamId,
      String status) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("status", status);
    enqueue(tenantId, FormalExamRealtimeTypes.FORCE_SUBMIT_COMPLETED,
        candidateExamId, examSessionId, candidateExamId, null, null,
        FormalExamRealtimeTypes.TARGET_SESSION, payload, null);
  }

  public void progress(
      Long tenantId,
      Long examSessionId,
      Long candidateExamId,
      Long accessSessionId,
      Integer generation,
      int answeredCount,
      Integer totalQuestionCount) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("answeredCount", answeredCount);
    payload.put("totalQuestionCount", totalQuestionCount);
    enqueue(tenantId, FormalExamRealtimeTypes.CANDIDATE_PROGRESS_CHANGED,
        candidateExamId, examSessionId, candidateExamId, accessSessionId, generation,
        FormalExamRealtimeTypes.TARGET_ACCESS, payload, null);
  }

  private void enqueue(
      Long tenantId,
      String type,
      Long aggregateId,
      Long examSessionId,
      Long candidateExamId,
      Long accessSessionId,
      Integer generation,
      String target,
      Map<String, Object> payload,
      Long commandId) {
    enqueue(tenantId, type, aggregateId, examSessionId, candidateExamId, accessSessionId,
        generation, target, payload, commandId, Instant.now(clock).plusSeconds(24 * 3600));
  }

  private void enqueue(
      Long tenantId,
      String type,
      Long aggregateId,
      Long examSessionId,
      Long candidateExamId,
      Long accessSessionId,
      Integer generation,
      String target,
      Map<String, Object> payload,
      Long commandId,
      Instant expiresAt) {
    if (tenantId == null || examSessionId == null || !StringUtils.hasText(type) || aggregateId == null) {
      return;
    }
    transactionTemplate.executeWithoutResult(status ->
        insertOutbox(
            tenantId, type, aggregateId, examSessionId, candidateExamId, accessSessionId,
            generation, target, payload, commandId, expiresAt));
  }

  /**
   * Cursor row lock is taken last, after the caller has already locked candidate/session rows.
   */
  private void insertOutbox(
      Long tenantId,
      String type,
      Long aggregateId,
      Long examSessionId,
      Long candidateExamId,
      Long accessSessionId,
      Integer generation,
      String target,
      Map<String, Object> payload,
      Long commandId,
      Instant expiresAt) {
    LocalDateTime now = LocalDateTime.now(clock);
    long seq = allocateSeq(tenantId, examSessionId, now);
    Map<String, Object> safePayload = payload == null ? Map.of() : new LinkedHashMap<>(payload);
    safePayload.put("target", target);
    String eventId = UUID.randomUUID().toString();
    CompetitionFormalExamOutboxEntity row = new CompetitionFormalExamOutboxEntity();
    row.setId(IdWorker.getId());
    row.setTenantId(tenantId);
    row.setEventId(eventId);
    row.setEventType(type);
    row.setAggregateId(aggregateId);
    row.setExamSessionId(examSessionId);
    row.setCandidateExamId(candidateExamId);
    row.setAccessSessionId(accessSessionId);
    row.setGeneration(generation);
    row.setSeq(seq);
    row.setPayloadJson(writeJson(envelope(
        eventId, commandId, type, aggregateId, examSessionId, candidateExamId,
        accessSessionId, generation, seq, expiresAt, target, safePayload)));
    row.setStatus("pending");
    row.setAttempts(0);
    row.setCreatedAt(now);
    row.setUpdatedAt(now);
    row.setDeleted(0);
    outboxMapper.insert(row);
  }

  private long allocateSeq(Long tenantId, Long examSessionId, LocalDateTime now) {
    cursorMapper.allocateNextSeq(IdWorker.getId(), tenantId, examSessionId, 0L, now);
    Long seq = cursorMapper.selectLastInsertId();
    if (seq == null || seq <= 0L) {
      throw new IllegalStateException("Formal exam realtime seq allocation failed");
    }
    return seq;
  }

  private FormalExamRealtimeEnvelope envelope(
      String eventId,
      Long commandId,
      String type,
      Long aggregateId,
      Long examSessionId,
      Long candidateExamId,
      Long accessSessionId,
      Integer generation,
      long seq,
      Instant expiresAt,
      String target,
      Map<String, Object> payload) {
    return new FormalExamRealtimeEnvelope(
        eventId,
        commandId == null ? null : String.valueOf(commandId),
        type,
        String.valueOf(aggregateId),
        String.valueOf(examSessionId),
        candidateExamId == null ? null : String.valueOf(candidateExamId),
        accessSessionId == null ? null : String.valueOf(accessSessionId),
        generation,
        seq,
        Instant.now(clock),
        expiresAt,
        target,
        payload);
  }

  private String writeJson(FormalExamRealtimeEnvelope envelope) {
    try {
      return objectMapper.writeValueAsString(envelope);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Formal exam realtime envelope json failed", ex);
    }
  }
}
