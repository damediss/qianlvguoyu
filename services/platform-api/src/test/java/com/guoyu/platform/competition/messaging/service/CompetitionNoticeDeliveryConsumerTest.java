package com.guoyu.platform.competition.messaging.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import com.guoyu.platform.competition.messaging.entity.CompetitionPlayerMessageEntity;
import com.guoyu.platform.competition.messaging.mapper.CompetitionPlayerMessageMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

class CompetitionNoticeDeliveryConsumerTest {

  private final CompetitionRegistrationMapper registrationMapper =
      Mockito.mock(CompetitionRegistrationMapper.class);
  private final CompetitionPlayerMessageMapper playerMessageMapper =
      Mockito.mock(CompetitionPlayerMessageMapper.class);
  private final CompetitionNoticeDeliveryConsumer consumer = new CompetitionNoticeDeliveryConsumer(
      new ObjectMapper().registerModule(new JavaTimeModule()), registrationMapper, playerMessageMapper);

  @AfterEach
  void clearTenantContext() {
    TenantContextHolder.clear();
  }

  @Test
  void createsInboxRowsOnlyForActiveEligiblePlayersAndClearsWorkerTenantContext() {
    when(registrationMapper.selectActivePlayerIdsByEvent(1L, 1001L)).thenReturn(List.of(2001L, 2002L));
    String payload = """
        {"tenantId":1,"eventId":1001,"noticeId":9001,"publishedAt":"2026-07-21T14:00:00"}
        """;

    consumer.consume(payload);

    ArgumentCaptor<CompetitionPlayerMessageEntity> captor =
        ArgumentCaptor.forClass(CompetitionPlayerMessageEntity.class);
    verify(playerMessageMapper, org.mockito.Mockito.times(2)).insertIgnoreDuplicate(captor.capture());
    assertThat(captor.getAllValues())
        .extracting(CompetitionPlayerMessageEntity::getPlayerId)
        .containsExactly(2001L, 2002L);
    assertThat(captor.getAllValues()).allSatisfy(message -> {
      assertThat(message.getTenantId()).isEqualTo(1L);
      assertThat(message.getEventId()).isEqualTo(1001L);
      assertThat(message.getNoticeId()).isEqualTo(9001L);
      assertThat(message.getType()).isEqualTo("notice_published");
      assertThat(message.getReceivedAt()).isNotNull();
    });
    assertThat(TenantContextHolder.getTenantId()).isNull();
  }

  @Test
  void acceptsEventWithoutRecipientsAndClearsWorkerTenantContext() {
    when(registrationMapper.selectActivePlayerIdsByEvent(1L, 1001L)).thenReturn(List.of());

    consumer.consume("""
        {"tenantId":1,"eventId":1001,"noticeId":9001,"publishedAt":"2026-07-21T14:00:00"}
        """);

    verifyNoInteractions(playerMessageMapper);
    assertThat(TenantContextHolder.getTenantId()).isNull();
  }

  @Test
  void rejectsMalformedPayloadWithoutLeakingTenantContext() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> consumer.consume("{\"tenantId\":1}"))
        .isInstanceOf(AmqpRejectAndDontRequeueException.class);

    verifyNoInteractions(registrationMapper, playerMessageMapper);
    assertThat(TenantContextHolder.getTenantId()).isNull();
  }
}
