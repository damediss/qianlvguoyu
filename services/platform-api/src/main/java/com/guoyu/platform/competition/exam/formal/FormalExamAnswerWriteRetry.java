package com.guoyu.platform.competition.exam.formal;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.SqlLockContention;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Retries an entire answer-write transaction on MySQL deadlock / lock-wait timeout.
 * Callers must invoke a <strong>new</strong> transactional proxy method each attempt;
 * never retry inside a transaction that is already rollback-only.
 */
public final class FormalExamAnswerWriteRetry {

  public static final int MAX_ATTEMPTS = 2;
  public static final int RETRY_AFTER_MS = 200;
  static final int SLEEP_MIN_MS = 20;
  static final int SLEEP_MAX_MS = 100;

  @FunctionalInterface
  public interface Sleeper {
    void sleep(long millis) throws InterruptedException;
  }

  public static Sleeper sleeper = Thread::sleep;

  private FormalExamAnswerWriteRetry() {
  }

  public static <T> T execute(Supplier<T> transactionalAttempt) {
    RuntimeException lastRetryable = null;
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        return transactionalAttempt.get();
      } catch (RuntimeException ex) {
        if (!isRetryable(ex)) {
          throw ex;
        }
        lastRetryable = ex;
        if (attempt >= MAX_ATTEMPTS) {
          break;
        }
        sleepJitter();
      }
    }
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("retryAfterMs", RETRY_AFTER_MS);
    BusinessException busy = new BusinessException(
        FormalExamErrorCodes.ANSWER_SYNC_RETRYABLE,
        "答案同步繁忙，请稍后重试",
        data);
    if (lastRetryable != null) {
      busy.initCause(lastRetryable);
    }
    throw busy;
  }

  public static boolean isRetryable(Throwable throwable) {
    return SqlLockContention.isRetryable(throwable);
  }

  private static void sleepJitter() {
    int span = SLEEP_MAX_MS - SLEEP_MIN_MS + 1;
    long delay = SLEEP_MIN_MS + ThreadLocalRandom.current().nextInt(span);
    try {
      sleeper.sleep(delay);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new BusinessException(
          FormalExamErrorCodes.ANSWER_SYNC_RETRYABLE,
          "答案同步繁忙，请稍后重试",
          Map.of("retryAfterMs", RETRY_AFTER_MS));
    }
  }
}
