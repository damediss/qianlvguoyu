package com.guoyu.platform.competition.exam.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Formal-exam capacity metrics. Tags are operation/outcome/sql-bucket only — never player names,
 * answers, phone numbers, or tokens.
 */
@Component
public class FormalExamMetrics {

  private final MeterRegistry meterRegistry;

  public FormalExamMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void recordHttp(String operation, String outcome, int sqlCount, Duration duration) {
    if (operation == null || operation.isBlank()) {
      return;
    }
    String safeOutcome = outcome == null || outcome.isBlank() ? "unknown" : outcome;
    Timer.builder("formal_exam.http")
        .description("Formal exam HTTP hot-path latency")
        .tag("operation", operation)
        .tag("outcome", safeOutcome)
        .tag("sql_bucket", sqlBucket(sqlCount))
        .register(meterRegistry)
        .record(duration);
    meterRegistry.counter("formal_exam.http.count", "operation", operation, "outcome", safeOutcome)
        .increment();
    meterRegistry.summary("formal_exam.sql.count", "operation", operation)
        .record(Math.max(0, sqlCount));
  }

  public void recordSyncBatch(int itemCount, int acceptedCount, int rejectedCount) {
    meterRegistry.summary("formal_exam.sync.items").record(Math.max(0, itemCount));
    meterRegistry.counter("formal_exam.sync.accepted").increment(Math.max(0, acceptedCount));
    meterRegistry.counter("formal_exam.sync.rejected").increment(Math.max(0, rejectedCount));
  }

  public void recordFinalize(String phase, int candidateCount, Duration duration) {
    String safePhase = phase == null || phase.isBlank() ? "unknown" : phase;
    Timer.builder("formal_exam.finalize")
        .tag("phase", safePhase)
        .register(meterRegistry)
        .record(duration);
    meterRegistry.summary("formal_exam.finalize.candidates", "phase", safePhase)
        .record(Math.max(0, candidateCount));
  }

  public void recordClaim(boolean acquired) {
    meterRegistry.counter("formal_exam.finalize.claim", "acquired", acquired ? "true" : "false")
        .increment();
  }

  public void recordWs(String operation, String outcome) {
    if (operation == null || operation.isBlank()) {
      return;
    }
    String safeOutcome = outcome == null || outcome.isBlank() ? "unknown" : outcome;
    meterRegistry.counter("formal_exam.ws", "operation", operation, "outcome", safeOutcome)
        .increment();
  }

  private static String sqlBucket(int sqlCount) {
    if (sqlCount <= 2) {
      return "0_2";
    }
    if (sqlCount <= 8) {
      return "3_8";
    }
    if (sqlCount <= 20) {
      return "9_20";
    }
    if (sqlCount <= 50) {
      return "21_50";
    }
    return "51_plus";
  }
}
