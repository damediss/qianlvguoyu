package com.guoyu.platform.competition.realtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.socket.WebSocketSession;

class CompetitionPracticalRealtimePublisherTest {

  @AfterEach
  void clearTransaction() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void publishAfterCommitWaitsUntilTransactionCommits() throws Exception {
    CompetitionPracticalRealtimePublisher publisher =
        new CompetitionPracticalRealtimePublisher(new ObjectMapper());
    WebSocketSession session = Mockito.mock(WebSocketSession.class);
    when(session.isOpen()).thenReturn(true);
    when(session.getAttributes()).thenReturn(Map.of("tenantId", 10159L));
    publisher.register(session);

    TransactionSynchronizationManager.initSynchronization();
    publisher.publishAfterCommit(10159L, "score-sheet-submitted", 501L);
    verify(session, never()).sendMessage(any());

    TransactionSynchronizationManager.getSynchronizations()
        .forEach(TransactionSynchronization::afterCommit);
    verify(session, times(1)).sendMessage(any());
  }
}
