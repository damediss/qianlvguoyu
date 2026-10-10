package com.guoyu.platform.competition.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Opt-in proof that candidate_exam row locks serialize answer writes without
 * answer-table gap locks. Run with {@code -Dcompetition.mysql.integration=true}.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "competition.mysql.integration", matches = "true")
class CompetitionFormalAnswerWriteMySqlTest {

  private static final int CANDIDATES = 100;
  private static final int QUESTIONS = 20;
  private static final long SHARED_QUESTION_ID = 1L;

  @Container
  static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
      .withDatabaseName("competition_test")
      .withUsername("test")
      .withPassword("test")
      .withCommand("--innodb-lock-wait-timeout=5", "--transaction-isolation=REPEATABLE-READ");

  @BeforeEach
  void createTables() throws Exception {
    try (Connection connection = connection(); Statement statement = connection.createStatement()) {
      statement.execute("DROP TABLE IF EXISTS competition_formal_exam_outbox");
      statement.execute("DROP TABLE IF EXISTS competition_formal_candidate_answer");
      statement.execute("DROP TABLE IF EXISTS competition_formal_candidate_exam");
      statement.execute("""
          CREATE TABLE competition_formal_candidate_exam (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            answered_count INT NOT NULL DEFAULT 0,
            deleted TINYINT NOT NULL DEFAULT 0
          ) ENGINE=InnoDB
          """);
      statement.execute("""
          CREATE TABLE competition_formal_candidate_answer (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            candidate_exam_id BIGINT NOT NULL,
            question_snapshot_id BIGINT NOT NULL,
            answer_version INT NOT NULL,
            answer_json VARCHAR(200) NOT NULL,
            is_answered TINYINT NOT NULL DEFAULT 1,
            deleted TINYINT NOT NULL DEFAULT 0,
            UNIQUE KEY uk_candidate_question (candidate_exam_id, question_snapshot_id, deleted)
          ) ENGINE=InnoDB
          """);
      statement.execute("""
          CREATE TABLE competition_formal_exam_outbox (
            id BIGINT PRIMARY KEY,
            exam_session_id BIGINT NOT NULL,
            seq BIGINT NOT NULL,
            payload VARCHAR(50) NOT NULL
          ) ENGINE=InnoDB
          """);
      try (PreparedStatement insert = connection.prepareStatement("""
          INSERT INTO competition_formal_candidate_exam (id, tenant_id, answered_count, deleted)
          VALUES (?, 1, 0, 0)
          """)) {
        for (int candidate = 1; candidate <= CANDIDATES; candidate++) {
          insert.setLong(1, candidate);
          insert.addBatch();
        }
        insert.executeBatch();
      }
    }
  }

  @Test
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  void oneHundredCandidatesFirstSaveSameQuestionWithoutDeadlock() throws Exception {
    RunStats stats = runConcurrent(CANDIDATES, (connection, candidateId) ->
        saveAnswer(connection, candidateId, SHARED_QUESTION_ID, 1, "A", false));

    assertThat(stats.deadlocks)
        .as("concurrent write details: %s%nLatest InnoDB status:%n%s",
            stats.details, stats.deadlocks == 0 ? "not requested" : innodbStatus())
        .isZero();
    assertThat(stats.failures).as("concurrent write details: %s", stats.details).isZero();
    assertThat(countAnswers()).isEqualTo(CANDIDATES);
    assertThat(countAnswersPerCandidateQuestion()).isEqualTo(CANDIDATES);
    assertAnsweredCountsMatch();
  }

  @RepeatedTest(10)
  @Timeout(value = 90, unit = TimeUnit.SECONDS)
  void eachCandidateSyncsTwentyQuestionsWithoutDeadlock() throws Exception {
    RunStats stats = runConcurrent(CANDIDATES, (connection, candidateId) -> {
      List<Long> questionIds = new ArrayList<>();
      for (long questionId = 1; questionId <= QUESTIONS; questionId++) {
        questionIds.add(questionId);
      }
      questionIds.sort(Long::compareTo);
      connection.setAutoCommit(false);
      lockCandidate(connection, candidateId);
      for (Long questionId : questionIds) {
        saveAnswerInOpenTx(connection, candidateId, questionId, 1, "A");
      }
      refreshAnsweredCount(connection, candidateId);
      insertOutbox(connection, candidateId, 1);
      connection.commit();
    });

    assertThat(stats.deadlocks)
        .as("concurrent write details: %s%nLatest InnoDB status:%n%s",
            stats.details, stats.deadlocks == 0 ? "not requested" : innodbStatus())
        .isZero();
    assertThat(stats.failures).as("concurrent write details: %s", stats.details).isZero();
    assertThat(countAnswers()).isEqualTo(CANDIDATES * QUESTIONS);
    assertThat(countAnswersPerCandidateQuestion()).isEqualTo(CANDIDATES * QUESTIONS);
    assertAnsweredCountsMatch();
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void sameCandidateConcurrentVersionsKeepHighestAccepted() throws Exception {
    RunStats stats = runConcurrentOnSameCandidate(1L, List.of(
        (connection, ignored) -> saveAnswer(connection, 1L, SHARED_QUESTION_ID, 1, "A", false),
        (connection, ignored) -> saveAnswer(connection, 1L, SHARED_QUESTION_ID, 2, "B", false)));

    assertThat(stats.deadlocks).as("concurrent write details: %s", stats.details).isZero();
    assertThat(stats.failures).as("concurrent write details: %s", stats.details).isZero();
    assertThat(countAnswers()).isEqualTo(1);
    assertThat(loadVersion(1L, SHARED_QUESTION_ID)).isEqualTo(2);
    assertThat(loadPayload(1L, SHARED_QUESTION_ID)).isEqualTo("B");
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void sameVersionSamePayloadIsIdempotent() throws Exception {
    try (Connection first = connection(); Connection second = connection()) {
      saveAnswer(first, 1L, SHARED_QUESTION_ID, 1, "A", false);
      saveAnswer(second, 1L, SHARED_QUESTION_ID, 1, "A", false);
    }

    assertThat(countAnswers()).isEqualTo(1);
    assertThat(loadVersion(1L, SHARED_QUESTION_ID)).isEqualTo(1);
    assertThat(loadPayload(1L, SHARED_QUESTION_ID)).isEqualTo("A");
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void sameVersionDifferentPayloadDoesNotOverwrite() throws Exception {
    try (Connection first = connection(); Connection second = connection()) {
      saveAnswer(first, 1L, SHARED_QUESTION_ID, 1, "A", false);
      saveAnswer(second, 1L, SHARED_QUESTION_ID, 1, "B", false);
    }

    assertThat(countAnswers()).isEqualTo(1);
    assertThat(loadPayload(1L, SHARED_QUESTION_ID)).isEqualTo("A");
    assertThat(loadVersion(1L, SHARED_QUESTION_ID)).isEqualTo(1);
  }

  @Test
  void rolledBackAnswerTransactionDoesNotLeaveOutbox() throws Exception {
    try (Connection connection = connection()) {
      saveAnswer(connection, 1L, SHARED_QUESTION_ID, 1, "A", true);
    }

    assertThat(countAnswers()).isZero();
    assertThat(countOutbox()).isZero();
  }

  private RunStats runConcurrent(int tasks, CandidateWork work) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(32);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger deadlocks = new AtomicInteger();
    AtomicInteger failures = new AtomicInteger();
    Queue<String> details = new ConcurrentLinkedQueue<>();
    List<Future<?>> futures = new ArrayList<>();
    try {
      for (int i = 1; i <= tasks; i++) {
        long candidateId = i;
        futures.add(pool.submit(() -> {
          try {
            if (!start.await(10, TimeUnit.SECONDS)) {
              failures.incrementAndGet();
              return;
            }
            try (Connection connection = connection()) {
              work.run(connection, candidateId);
            }
          } catch (SQLException ex) {
            details.add(sqlFailure(candidateId, ex));
            if (ex.getErrorCode() == 1213 || "40001".equals(ex.getSQLState())) {
              deadlocks.incrementAndGet();
            } else {
              failures.incrementAndGet();
            }
          } catch (Exception ex) {
            details.add("candidateId=" + candidateId + ", " + ex);
            failures.incrementAndGet();
          }
        }));
      }
      start.countDown();
      for (Future<?> future : futures) {
        future.get(60, TimeUnit.SECONDS);
      }
    } finally {
      pool.shutdownNow();
    }
    return new RunStats(deadlocks.get(), failures.get(), List.copyOf(details));
  }

  private RunStats runConcurrentOnSameCandidate(long candidateId, List<CandidateWork> workers)
      throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(workers.size());
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger deadlocks = new AtomicInteger();
    AtomicInteger failures = new AtomicInteger();
    Queue<String> details = new ConcurrentLinkedQueue<>();
    List<Future<?>> futures = new ArrayList<>();
    try {
      for (CandidateWork work : workers) {
        futures.add(pool.submit(() -> {
          try {
            if (!start.await(10, TimeUnit.SECONDS)) {
              failures.incrementAndGet();
              return;
            }
            try (Connection connection = connection()) {
              work.run(connection, candidateId);
            }
          } catch (SQLException ex) {
            details.add(sqlFailure(candidateId, ex));
            if (ex.getErrorCode() == 1213 || "40001".equals(ex.getSQLState())) {
              deadlocks.incrementAndGet();
            } else {
              failures.incrementAndGet();
            }
          } catch (Exception ex) {
            details.add("candidateId=" + candidateId + ", " + ex);
            failures.incrementAndGet();
          }
        }));
      }
      start.countDown();
      for (Future<?> future : futures) {
        future.get(30, TimeUnit.SECONDS);
      }
    } finally {
      pool.shutdownNow();
    }
    return new RunStats(deadlocks.get(), failures.get(), List.copyOf(details));
  }

  private void saveAnswer(
      Connection connection,
      long candidateId,
      long questionId,
      int version,
      String payload,
      boolean rollback) throws SQLException {
    boolean opened = connection.getAutoCommit();
    if (opened) {
      connection.setAutoCommit(false);
    }
    try {
      lockCandidate(connection, candidateId);
      boolean mutated = saveAnswerInOpenTx(connection, candidateId, questionId, version, payload);
      if (mutated) {
        refreshAnsweredCount(connection, candidateId);
        insertOutbox(connection, candidateId, version);
      }
      if (rollback) {
        connection.rollback();
      } else {
        connection.commit();
      }
    } catch (SQLException ex) {
      connection.rollback();
      throw ex;
    } finally {
      if (opened) {
        connection.setAutoCommit(true);
      }
    }
  }

  private boolean saveAnswerInOpenTx(
      Connection connection,
      long candidateId,
      long questionId,
      int version,
      String payload) throws SQLException {
    Integer currentVersion = null;
    try (PreparedStatement select = connection.prepareStatement("""
        SELECT answer_version, answer_json
        FROM competition_formal_candidate_answer
        WHERE candidate_exam_id = ? AND question_snapshot_id = ? AND deleted = 0
        """)) {
      select.setLong(1, candidateId);
      select.setLong(2, questionId);
      try (ResultSet rs = select.executeQuery()) {
        if (rs.next()) {
          currentVersion = rs.getInt(1);
        }
      }
    }
    if (currentVersion == null) {
      try (PreparedStatement insert = connection.prepareStatement("""
          INSERT INTO competition_formal_candidate_answer
            (id, tenant_id, candidate_exam_id, question_snapshot_id, answer_version, answer_json, is_answered, deleted)
          VALUES (?, 1, ?, ?, ?, ?, 1, 0)
          """)) {
        insert.setLong(1, answerId(candidateId, questionId));
        insert.setLong(2, candidateId);
        insert.setLong(3, questionId);
        insert.setInt(4, version);
        insert.setString(5, payload);
        insert.executeUpdate();
      }
      return true;
    }
    if (version < currentVersion) {
      return false;
    }
    if (version == currentVersion) {
      return false;
    }
    try (PreparedStatement update = connection.prepareStatement("""
        UPDATE competition_formal_candidate_answer
        SET answer_version = ?, answer_json = ?
        WHERE candidate_exam_id = ? AND question_snapshot_id = ? AND deleted = 0
        """)) {
      update.setInt(1, version);
      update.setString(2, payload);
      update.setLong(3, candidateId);
      update.setLong(4, questionId);
      update.executeUpdate();
    }
    return true;
  }

  private void lockCandidate(Connection connection, long candidateId) throws SQLException {
    try (PreparedStatement lock = connection.prepareStatement("""
        SELECT id FROM competition_formal_candidate_exam WHERE id = ? AND deleted = 0 FOR UPDATE
        """)) {
      lock.setLong(1, candidateId);
      try (ResultSet rs = lock.executeQuery()) {
        assertThat(rs.next()).isTrue();
      }
    }
  }

  private void refreshAnsweredCount(Connection connection, long candidateId) throws SQLException {
    int answeredCount;
    try (PreparedStatement count = connection.prepareStatement("""
        SELECT COUNT(*) FROM competition_formal_candidate_answer
        WHERE candidate_exam_id = ? AND is_answered = 1 AND deleted = 0
        """)) {
      count.setLong(1, candidateId);
      try (ResultSet rs = count.executeQuery()) {
        assertThat(rs.next()).isTrue();
        answeredCount = rs.getInt(1);
      }
    }
    // Mirror production: a non-locking SELECT COUNT followed by an update of the
    // already locked candidate row. Do not fold the count into an UPDATE subquery;
    // MySQL treats that DML subquery as a locking read and can deadlock across candidates.
    try (PreparedStatement update = connection.prepareStatement("""
        UPDATE competition_formal_candidate_exam SET answered_count = ? WHERE id = ?
        """)) {
      update.setInt(1, answeredCount);
      update.setLong(2, candidateId);
      update.executeUpdate();
    }
  }

  private void insertOutbox(Connection connection, long candidateId, int seq) throws SQLException {
    try (PreparedStatement insert = connection.prepareStatement("""
        INSERT INTO competition_formal_exam_outbox (id, exam_session_id, seq, payload)
        VALUES (?, 9001, ?, 'progress')
        """)) {
      insert.setLong(1, candidateId * 10_000L + seq);
      insert.setInt(2, seq);
      insert.executeUpdate();
    }
  }

  private void assertAnsweredCountsMatch() throws SQLException {
    try (Connection connection = connection();
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("""
            SELECT e.id, e.answered_count, COUNT(a.id)
            FROM competition_formal_candidate_exam e
            LEFT JOIN competition_formal_candidate_answer a
              ON a.candidate_exam_id = e.id AND a.is_answered = 1 AND a.deleted = 0
            GROUP BY e.id, e.answered_count
            HAVING e.answered_count <> COUNT(a.id)
            """)) {
      assertThat(rs.next()).isFalse();
    }
  }

  private int countAnswers() throws SQLException {
    return scalar("SELECT COUNT(*) FROM competition_formal_candidate_answer WHERE deleted = 0");
  }

  private int countAnswersPerCandidateQuestion() throws SQLException {
    return scalar("""
        SELECT COUNT(*) FROM (
          SELECT candidate_exam_id, question_snapshot_id
          FROM competition_formal_candidate_answer
          WHERE deleted = 0
          GROUP BY candidate_exam_id, question_snapshot_id
        ) t
        """);
  }

  private int countOutbox() throws SQLException {
    return scalar("SELECT COUNT(*) FROM competition_formal_exam_outbox");
  }

  private int loadVersion(long candidateId, long questionId) throws SQLException {
    return scalar(
        "SELECT answer_version FROM competition_formal_candidate_answer WHERE candidate_exam_id = "
            + candidateId + " AND question_snapshot_id = " + questionId);
  }

  private String loadPayload(long candidateId, long questionId) throws SQLException {
    try (Connection connection = connection();
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(
            "SELECT answer_json FROM competition_formal_candidate_answer WHERE candidate_exam_id = "
                + candidateId + " AND question_snapshot_id = " + questionId)) {
      assertThat(rs.next()).isTrue();
      return rs.getString(1);
    }
  }

  private int scalar(String sql) throws SQLException {
    try (Connection connection = connection();
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql)) {
      assertThat(rs.next()).isTrue();
      return rs.getInt(1);
    }
  }

  private Connection connection() throws SQLException {
    return DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
  }

  private String innodbStatus() {
    try (Connection connection = DriverManager.getConnection(
            MYSQL.getJdbcUrl(), "root", MYSQL.getPassword());
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SHOW ENGINE INNODB STATUS")) {
      return rs.next() ? rs.getString("Status") : "SHOW ENGINE INNODB STATUS returned no row";
    } catch (SQLException ex) {
      return "Unable to read InnoDB status: " + ex;
    }
  }

  private long answerId(long candidateId, long questionId) {
    return candidateId * 1000 + questionId;
  }

  @FunctionalInterface
  private interface CandidateWork {
    void run(Connection connection, long candidateId) throws Exception;
  }

  private static String sqlFailure(long candidateId, SQLException ex) {
    return "candidateId=" + candidateId
        + ", errorCode=" + ex.getErrorCode()
        + ", sqlState=" + ex.getSQLState()
        + ", message=" + ex.getMessage();
  }

  private record RunStats(int deadlocks, int failures, List<String> details) {
  }
}
