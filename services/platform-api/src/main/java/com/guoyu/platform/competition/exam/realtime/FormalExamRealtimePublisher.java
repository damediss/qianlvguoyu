package com.guoyu.platform.competition.exam.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.competition.entity.CompetitionFormalExamOutboxEntity;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamOutboxMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class FormalExamRealtimePublisher {

  private final CompetitionFormalExamOutboxMapper outboxMapper;
  private final FormalExamOutboxDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public FormalExamRealtimePublisher(
      CompetitionFormalExamOutboxMapper outboxMapper,
      FormalExamOutboxDispatcher dispatcher,
      ObjectMapper objectMapper,
      Clock clock) {
    this.outboxMapper = outboxMapper;
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.clock = clock;
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
    if (org.springframework.util.StringUtils.hasText(previousStatus)) {
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
    LocalDateTime now = LocalDateTime.now(clock);
    Integer maxSeq = outboxMapper.selectMaxSeq(examSessionId);
    int seq = (maxSeq == null ? 0 : maxSeq) + 1;
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
    Long outboxId = row.getId();
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          dispatcher.publishOne(outboxId);
        }
      });
    } else {
      dispatcher.publishOne(outboxId);
    }
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
      int seq,
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
