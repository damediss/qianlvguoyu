package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalScoringTaskEntity;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalScoringTaskMapper;
import com.guoyu.platform.competition.messaging.config.CompetitionScoringMessagingProperties;
import com.guoyu.platform.competition.messaging.entity.CompetitionOutboxEventEntity;
import com.guoyu.platform.competition.messaging.mapper.CompetitionOutboxEventMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CompetitionFormalScoringTaskServiceTest {
  private final CompetitionFormalScoringTaskMapper tasks =
      Mockito.mock(CompetitionFormalScoringTaskMapper.class);
  private final CompetitionOutboxEventMapper outbox =
      Mockito.mock(CompetitionOutboxEventMapper.class);
  private final CompetitionFormalExamSessionMapper sessions =
      Mockito.mock(CompetitionFormalExamSessionMapper.class);
  private final CompetitionFormalCandidateExamMapper candidates =
      Mockito.mock(CompetitionFormalCandidateExamMapper.class);
  private CompetitionFormalScoringTaskService service;

  @BeforeEach
  void setup() {
    CompetitionScoringMessagingProperties properties = new CompetitionScoringMessagingProperties();
    properties.setMaxAttempts(3);
    service = new CompetitionFormalScoringTaskService(tasks, outbox, sessions, candidates,
        properties, new ObjectMapper());
    when(tasks.insert(any(CompetitionFormalScoringTaskEntity.class))).thenReturn(1);
    when(outbox.insert(any(CompetitionOutboxEventEntity.class))).thenReturn(1);
  }

  @Test
  void enqueueCreatesOneTaskAndDurableOutboxEvent() {
    CompetitionFormalCandidateExamEntity candidate = candidate();

    CompetitionFormalScoringTaskEntity task =
        service.enqueueInitial(candidate, "auto-submit", 7001L);

    assertThat(task.getTaskStatus()).isEqualTo("pending");
    assertThat(task.getResultRevisionNo()).isEqualTo(1);
    ArgumentCaptor<CompetitionOutboxEventEntity> eventCaptor =
        ArgumentCaptor.forClass(CompetitionOutboxEventEntity.class);
    verify(outbox).insert(eventCaptor.capture());
    CompetitionOutboxEventEntity event = eventCaptor.getValue();
    assertThat(event.getEventChannel()).isEqualTo("scoring");
    assertThat(event.getRoutingKey()).isEqualTo("competition.formal-score.requested.v1");
    assertThat(event.getEventKey()).contains("formal-score:").endsWith(":delivery:0");
    assertThat(event.getPayloadJson()).contains("\"tenantId\":10159")
        .contains("\"candidateExamId\":9101");
  }

  @Test
  void duplicateRevisionReusesExistingTaskWithoutAnotherOutbox() {
    CompetitionFormalScoringTaskEntity existing = task(1);
    when(tasks.selectByCandidateAndRevision(9101L, 1)).thenReturn(existing);

    assertThat(service.enqueueInitial(candidate(), "duplicate", 7001L)).isSameAs(existing);

    verify(tasks, never()).insert(any(CompetitionFormalScoringTaskEntity.class));
    verify(outbox, never()).insert(any(CompetitionOutboxEventEntity.class));
  }

  @Test
  void failureSchedulesRetryThenStopsAtThirdAttempt() {
    CompetitionFormalScoringTaskEntity first = task(1);
    when(tasks.fail(anyLong(), anyString(), anyString(), any(LocalDateTime.class),
        anyString(), any(LocalDateTime.class))).thenReturn(1);
    service.fail(first, "worker-1", new IllegalStateException("boom"));
    ArgumentCaptor<CompetitionOutboxEventEntity> eventCaptor =
        ArgumentCaptor.forClass(CompetitionOutboxEventEntity.class);
    verify(outbox).insert(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getEventKey()).endsWith(":delivery:1");

    Mockito.reset(outbox);
    CompetitionFormalScoringTaskEntity terminal = task(3);
    service.fail(terminal, "worker-1", new IllegalStateException("still broken"));
    verify(outbox, never()).insert(any(CompetitionOutboxEventEntity.class));
  }

  private CompetitionFormalCandidateExamEntity candidate() {
    CompetitionFormalCandidateExamEntity candidate = new CompetitionFormalCandidateExamEntity();
    candidate.setId(9101L);
    candidate.setTenantId(10159L);
    candidate.setExamSessionId(9001L);
    candidate.setStatus("auto_submitted");
    return candidate;
  }

  private CompetitionFormalScoringTaskEntity task(int attempts) {
    CompetitionFormalScoringTaskEntity task = new CompetitionFormalScoringTaskEntity();
    task.setId(9201L);
    task.setTenantId(10159L);
    task.setExamSessionId(9001L);
    task.setCandidateExamId(9101L);
    task.setResultRevisionNo(1);
    task.setTaskStatus("processing");
    task.setAttemptCount(attempts);
    task.setRequestedBy(7001L);
    return task;
  }
}
