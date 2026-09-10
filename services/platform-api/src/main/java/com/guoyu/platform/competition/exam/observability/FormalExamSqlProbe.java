package com.guoyu.platform.competition.exam.observability;

/**
 * Request-scoped SQL counter for formal-exam hot paths. Counts statements only; never stores
 * answers, tokens, or personal data.
 */
public final class FormalExamSqlProbe {

  private static final ThreadLocal<Counter> ACTIVE = new ThreadLocal<>();

  private FormalExamSqlProbe() {
  }

  public static void enter() {
    ACTIVE.set(new Counter());
  }

  public static void increment() {
    Counter counter = ACTIVE.get();
    if (counter != null) {
      counter.count++;
    }
  }

  public static int exit() {
    Counter counter = ACTIVE.get();
    ACTIVE.remove();
    return counter == null ? 0 : counter.count;
  }

  public static boolean isActive() {
    return ACTIVE.get() != null;
  }

  private static final class Counter {
    private int count;
  }
}
