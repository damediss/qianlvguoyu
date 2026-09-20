package com.guoyu.platform.competition.messaging.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionFormalScoringTaskEntity;
import com.guoyu.platform.competition.messaging.model.FormalExamScoringRequestedEvent;
import com.guoyu.platform.competition.service.CompetitionFormalExamCandidateScoringService;
import com.guoyu.platform.competition.service.CompetitionFormalScoringTaskService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "platform.competition.formal-exam.scoring-messaging",
    name = "enabled", havingValue = "true", matchIfMissing = false)
public class CompetitionFormalScoringConsumer {
  private final ObjectMapper objectMapper;
  private final CompetitionFormalScoringTaskService taskService;
  private final CompetitionFormalExamCandidateScoringService scoringService;
  private final String workerId = UUID.randomUUID().toString();

  public CompetitionFormalScoringConsumer(ObjectMapper objectMapper,
      CompetitionFormalScoringTaskService taskService,
      CompetitionFormalExamCandidateScoringService scoringService) {
    this.objectMapper = objectMapper;
    this.taskService = taskService;
    this.scoringService = scoringService;
  }

  @RabbitListener(queues = "#{@competitionFormalScoringQueue.name}",
      containerFactory = "competitionScoringListenerFactory")
  public void consume(String payload) {
    FormalExamScoringRequestedEvent event;
    try {
      event = objectMapper.readValue(payload, FormalExamScoringRequestedEvent.class);
    } catch (Exception ex) {
      log.error("Invalid formal scoring event payload", ex);
      throw new AmqpRejectAndDontRequeueException("invalid formal scoring payload", ex);
    }
    if (event.tenantId() == null || event.taskId() == null) {
      log.error("Formal scoring event is missing tenantId/taskId");
      throw new AmqpRejectAndDontRequeueException("formal scoring event missing tenantId/taskId");
    }
    TenantContextHolder.setTenantId(event.tenantId());
    String owner = workerId + ":" + Thread.currentThread().threadId();
    CompetitionFormalScoringTaskEntity task = null;
    try {
      task = taskService.claim(event.taskId(), owner);
      if (task == null) {
        return;
      }
      scoringService.scoreCandidate(task.getExamSessionId(), task.getCandidateExamId(),
          task.getResultRevisionNo(), task.getRequestedReason(), LocalDateTime.now());
      taskService.complete(task, owner);
    } catch (RuntimeException ex) {
      log.error("Formal scoring worker failed, tenantId={}, taskId={}, candidateExamId={}",
          event.tenantId(), event.taskId(), event.candidateExamId(), ex);
      if (task != null) {
        taskService.fail(task, owner, ex);
      }
    } finally {
      TenantContextHolder.clear();
    }
  }
}
