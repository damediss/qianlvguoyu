package com.guoyu.platform.competition.exam.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalExamOutboxEntity;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamOutboxMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamRealtimeCursorMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

class FormalExamRealtimePublisherTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-10T01:00:00Z"), ZoneId.of("Asia/Shanghai"));

  @Test
  void allocateSeqUsesCursorThenInsertsOutbox() {
    CompetitionFormalExamOutboxMapper outbox = Mockito.mock(CompetitionFormalExamOutboxMapper.class);
    CompetitionFormalExamRealtimeCursorMapper cursor =
        Mockito.mock(CompetitionFormalExamRealtimeCursorMapper.class);
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    when(cursor.selectLastInsertId()).thenReturn(7L);

    FormalExamRealtimePublisher publisher = new FormalExamRealtimePublisher(
        outbox, cursor, objectMapper, CLOCK, noopTx());
    publisher.progress(10159L, 9001L, 9101L, 100L, 1, 3, 10);

    verify(cursor).allocateNextSeq(any(), eq(10159L), eq(9001L), eq(0L), any(LocalDateTime.class));
    verify(cursor).selectLastInsertId();
    ArgumentCaptor<CompetitionFormalExamOutboxEntity> captor =
        ArgumentCaptor.forClass(CompetitionFormalExamOutboxEntity.class);
    verify(outbox).insert(captor.capture());
    assertThat(captor.getValue().getSeq()).isEqualTo(7L);
    assertThat(captor.getValue().getPayloadJson()).contains("\"seq\":\"7\"");
  }

  private static PlatformTransactionManager noopTx() {
    return new PlatformTransactionManager() {
      @Override
      public TransactionStatus getTransaction(TransactionDefinition definition) {
        return new SimpleTransactionStatus();
      }

      @Override
      public void commit(TransactionStatus status) {
      }

      @Override
      public void rollback(TransactionStatus status) {
      }
    };
  }
}
