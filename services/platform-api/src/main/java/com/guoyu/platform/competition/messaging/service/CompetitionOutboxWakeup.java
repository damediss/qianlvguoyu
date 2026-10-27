package com.guoyu.platform.competition.messaging.service;

import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Non-blocking local wakeup; the scheduled scan remains the durable recovery path. */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "platform.competition.messaging", name = "enabled", havingValue = "true")
public class CompetitionOutboxWakeup {

  private final CompetitionOutboxDispatcher dispatcher;
  private final ThreadPoolTaskExecutor executor;

  public CompetitionOutboxWakeup(
      CompetitionOutboxDispatcher dispatcher,
      @Qualifier("competitionOutboxWakeupExecutor") ThreadPoolTaskExecutor executor) {
    this.dispatcher = dispatcher;
    this.executor = executor;
  }

  public void wakeAfterCommit() {
    Runnable wake = () -> {
      try {
        executor.execute(dispatcher::dispatch);
      } catch (RejectedExecutionException ex) {
        log.warn("Competition outbox wakeup queue is full; scheduled scan will recover");
      }
    };
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          wake.run();
        }
      });
    } else {
      wake.run();
    }
  }
}
