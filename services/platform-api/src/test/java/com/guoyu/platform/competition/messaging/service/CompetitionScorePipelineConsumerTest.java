package com.guoyu.platform.competition.messaging.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.mapper.CompetitionScoreSourceMapper;
import com.guoyu.platform.competition.messaging.config.CompetitionScoringMessagingProperties;
import com.guoyu.platform.competition.service.CompetitionAutomaticScorePipelineService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

class CompetitionScorePipelineConsumerTest {
  private final CompetitionScoreSourceMapper sources =
      Mockito.mock(CompetitionScoreSourceMapper.class);
  private final CompetitionAutomaticScorePipelineService pipeline =
      Mockito.mock(CompetitionAutomaticScorePipelineService.class);
  private final CompetitionScorePipelineOutboxService outbox =
      Mockito.mock(CompetitionScorePipelineOutboxService.class);

  @AfterEach
  void clearTenant() {
    TenantContextHolder.clear();
  }

  @Test
  void processingFailureCreatesDurableRetryAndAlwaysClearsTenantContext() {
    CompetitionScoreSourceEntity source = new CompetitionScoreSourceEntity();
    source.setId(8101L);
    source.setTenantId(10159L);
    source.setConfirmationStatus("confirmed");
    source.setConfirmedBy(7001L);
    when(sources.selectById(8101L)).thenReturn(source);
    doThrow(new IllegalStateException("temporary database failure"))
        .when(pipeline).onConfirmedSource(source, 7001L);
    CompetitionScoringMessagingProperties properties = new CompetitionScoringMessagingProperties();
    properties.setMaxAttempts(3);
    CompetitionScorePipelineConsumer consumer = new CompetitionScorePipelineConsumer(
        new ObjectMapper(), sources, pipeline, outbox, properties);

    assertThatThrownBy(() -> consumer.consume(
        "{\"tenantId\":10159,\"scoreSourceId\":8101,\"attemptNo\":0}"))
        .isInstanceOf(AmqpRejectAndDontRequeueException.class);

    verify(outbox).enqueueRetry(source, 7001L, 1);
    assertThat(TenantContextHolder.getTenantId()).isNull();
  }
}
