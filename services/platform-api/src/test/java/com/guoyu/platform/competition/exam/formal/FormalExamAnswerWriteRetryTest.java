package com.guoyu.platform.competition.exam.formal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.common.exception.BusinessException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DeadlockLoserDataAccessException;

class FormalExamAnswerWriteRetryTest {

  @AfterEach
  void restoreSleeper() {
    FormalExamAnswerWriteRetry.sleeper = Thread::sleep;
  }

  @Test
  void retriesOnceOnDeadlockThenReturns() {
    List<Long> sleeps = new ArrayList<>();
    FormalExamAnswerWriteRetry.sleeper = sleeps::add;
    AtomicInteger calls = new AtomicInteger();

    String result = FormalExamAnswerWriteRetry.execute(() -> {
      if (calls.incrementAndGet() == 1) {
        throw deadlock();
      }
      return "ok";
    });

    assertThat(result).isEqualTo("ok");
    assertThat(calls.get()).isEqualTo(2);
    assertThat(sleeps).hasSize(1);
    assertThat(sleeps.getFirst()).isBetween(20L, 100L);
  }

  @Test
  void twoDeadlocksBecomeRetryableBusinessError() {
    FormalExamAnswerWriteRetry.sleeper = millis -> {
    };
    AtomicInteger calls = new AtomicInteger();

    assertThatThrownBy(() -> FormalExamAnswerWriteRetry.execute(() -> {
      calls.incrementAndGet();
      throw deadlock();
    }))
        .isInstanceOf(BusinessException.class)
        .hasMessage("答案同步繁忙，请稍后重试")
        .extracting("code")
        .isEqualTo(FormalExamErrorCodes.ANSWER_SYNC_RETRYABLE);
    assertThat(calls.get()).isEqualTo(2);
  }

  @Test
  void nonRetryableErrorsAreNotSwallowed() {
    assertThatThrownBy(() -> FormalExamAnswerWriteRetry.execute(() -> {
      throw new BusinessException("409", "版本冲突");
    }))
        .isInstanceOf(BusinessException.class)
        .hasMessage("版本冲突");
  }

  private static DeadlockLoserDataAccessException deadlock() {
    return new DeadlockLoserDataAccessException(
        "Deadlock found when trying to get lock",
        new SQLException("Deadlock found when trying to get lock", "40001", 1213));
  }
}
