package com.guoyu.platform.competition.messaging.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.messaging.config.CompetitionScoringMessagingProperties;
import com.guoyu.platform.competition.messaging.entity.CompetitionOutboxEventEntity;
import com.guoyu.platform.competition.messaging.mapper.CompetitionOutboxEventMapper;
import com.guoyu.platform.competition.messaging.model.CompetitionScorePipelineRequestedEvent;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetitionScorePipelineOutboxService {
  private final CompetitionOutboxEventMapper mapper;
  private final CompetitionScoringMessagingProperties properties;
  private final ObjectMapper objectMapper;

  public CompetitionScorePipelineOutboxService(CompetitionOutboxEventMapper mapper,
      CompetitionScoringMessagingProperties properties, ObjectMapper objectMapper) {
    this.mapper = mapper;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public void enqueue(CompetitionScoreSourceEntity source, Long operatorId) {
    enqueue(source, operatorId, 0, LocalDateTime.now());
  }

  @Transactional
  public void enqueueRetry(CompetitionScoreSourceEntity source, Long operatorId, int attemptNo) {
    long delaySeconds = attemptNo <= 1 ? 1L : attemptNo == 2 ? 5L : 30L;
    enqueue(source, operatorId, attemptNo, LocalDateTime.now().plusSeconds(delaySeconds));
  }

  private void enqueue(CompetitionScoreSourceEntity source, Long operatorId, int attemptNo,
      LocalDateTime availableAt) {
    String key = "score-pipeline:source:" + source.getId()
        + (attemptNo == 0 ? "" : ":retry:" + attemptNo);
    CompetitionOutboxEventEntity existing = mapper.selectOne(
        new LambdaQueryWrapper<CompetitionOutboxEventEntity>()
            .eq(CompetitionOutboxEventEntity::getEventKey, key)
            .eq(CompetitionOutboxEventEntity::getDeleted, 0).last("LIMIT 1"));
    if (existing != null) return;
    LocalDateTime now = LocalDateTime.now();
    CompetitionOutboxEventEntity event = new CompetitionOutboxEventEntity();
    event.setId(IdWorker.getId()); event.setTenantId(source.getTenantId());
    event.setEventChannel("score_pipeline"); event.setAggregateType("competition_score_source");
    event.setAggregateId(source.getId()); event.setEventType("competition.score-source.confirmed.v1");
    event.setRoutingKey(properties.getPipelineRoutingKey()); event.setEventKey(key);
    try {
      event.setPayloadJson(objectMapper.writeValueAsString(
          new CompetitionScorePipelineRequestedEvent(source.getTenantId(), source.getId(), attemptNo)));
    } catch (JsonProcessingException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "成绩计算事件序列化失败");
    }
    event.setStatus("pending"); event.setAttemptCount(0); event.setNextAttemptAt(availableAt);
    event.setCreatedBy(operatorId); event.setUpdatedBy(operatorId);
    event.setCreatedAt(now); event.setUpdatedAt(now); event.setDeleted(0);
    if (mapper.insert(event) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "成绩计算 Outbox 创建失败");
    }
  }
}
