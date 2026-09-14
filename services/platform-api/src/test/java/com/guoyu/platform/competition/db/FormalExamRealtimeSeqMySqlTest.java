package com.guoyu.platform.competition.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@EnabledIfSystemProperty(named = "competition.mysql.integration", matches = "true")
class FormalExamRealtimeSeqMySqlTest {

  private static final int WRITES = 1000;
  private static final long TENANT = 10159L;
  private static final long SESSION = 9001L;

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
      statement.execute("DROP TABLE IF EXISTS competition_formal_exam_realtime_cursor");
      statement.execute("""
          CREATE TABLE competition_formal_exam_realtime_cursor (
            id BIGINT NOT NULL PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            exam_session_id BIGINT NOT NULL,
            last_seq BIGINT NOT NULL DEFAULT 0,
            created_by BIGINT NULL,
            updated_by BIGINT NULL,
            created_at DATETIME NOT NULL,
            updated_at DATETIME NOT NULL,
            deleted TINYINT NOT NULL DEFAULT 0,
            UNIQUE KEY uk_session (tenant_id, exam_session_id, deleted)
          ) ENGINE=InnoDB
          """);
      statement.execute("""
          CREATE TABLE competition_formal_exam_outbox (
            id BIGINT NOT NULL PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            event_id VARCHAR(64) NOT NULL,
            event_type VARCHAR(64) NOT NULL,
            aggregate_id BIGINT NOT NULL,
            exam_session_id BIGINT NOT NULL,
            seq BIGINT NOT NULL,
            payload_json TEXT NOT NULL,
            status VARCHAR(32) NOT NULL,
            attempts INT NOT NULL DEFAULT 0,
            published_at DATETIME NULL,
            last_error VARCHAR(500) NULL,
            created_by BIGINT NULL,
            updated_by BIGINT NULL,
            created_at DATETIME NOT NULL,
            updated_at DATETIME NOT NULL,
            deleted TINYINT NOT NULL DEFAULT 0,
            UNIQUE KEY uk_event (tenant_id, event_id, deleted),
            UNIQUE KEY uk_seq (tenant_id, exam_session_id, seq, deleted)
          ) ENGINE=InnoDB
          """);
    }
  }

  @Test
  @Timeout(value = 90, unit = TimeUnit.SECONDS)
  void twoNodesAllocateOneThousandUniqueContiguousSeq() throws Exception {
    AtomicInteger deadlocks = new AtomicInteger();
    AtomicInteger lockWaits = new AtomicInteger();
    AtomicInteger failures = new AtomicInteger();
    ConcurrentLinkedQueue<String> details = new ConcurrentLinkedQueue<>();
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService node1 = Executors.newFixedThreadPool(8);
    ExecutorService node2 = Executors.newFixedThreadPool(8);
    List<Future<?>> futures = new ArrayList<>();
    try {
      for (int i = 1; i <= WRITES; i++) {
        int index = i;
        ExecutorService pool = index % 2 == 0 ? node1 : node2;
        futures.add(pool.submit(() -> {
          try {
            start.await();
            insertOutbox(index);
          } catch (SQLException ex) {
            details.add(ex.getErrorCode() + " " + ex.getMessage());
            if (ex.getErrorCode() == 1213) {
              deadlocks.incrementAndGet();
            } else if (ex.getErrorCode() == 1205) {
              lockWaits.incrementAndGet();
            } else {
              failures.incrementAndGet();
            }
          } catch (Exception ex) {
            failures.incrementAndGet();
            details.add(String.valueOf(ex));
          }
        }));
      }
      start.countDown();
      for (Future<?> future : futures) {
        future.get(60, TimeUnit.SECONDS);
      }
    } finally {
      node1.shutdownNow();
      node2.shutdownNow();
    }
    assertThat(deadlocks.get()).as("details=%s", details).isZero();
    assertThat(lockWaits.get()).as("details=%s", details).isZero();
    assertThat(failures.get()).as("details=%s", details).isZero();
    assertThat(count("SELECT COUNT(*) FROM competition_formal_exam_outbox WHERE deleted=0"))
        .isEqualTo(WRITES);
    assertThat(count("SELECT COUNT(DISTINCT seq) FROM competition_formal_exam_outbox WHERE deleted=0"))
        .isEqualTo(WRITES);
    assertThat(count("SELECT COUNT(DISTINCT event_id) FROM competition_formal_exam_outbox WHERE deleted=0"))
        .isEqualTo(WRITES);
    assertThat(count("SELECT MAX(seq) FROM competition_formal_exam_outbox WHERE deleted=0"))
        .isEqualTo(WRITES);
    assertThat(count("SELECT MIN(seq) FROM competition_formal_exam_outbox WHERE deleted=0"))
        .isEqualTo(1);
    assertThat(count("""
        SELECT COUNT(*) FROM (
          SELECT seq FROM competition_formal_exam_outbox WHERE deleted=0 GROUP BY seq HAVING COUNT(*) > 1
        ) t
        """)).isZero();
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void outboxInsertFailureRollsBackCursorIncrement() throws Exception {
    try (Connection connection = connection()) {
      connection.setAutoCommit(false);
      long seq = allocate(connection, 1L);
      assertThat(seq).isEqualTo(1L);
      connection.rollback();
    }
    assertThat(count("SELECT COUNT(*) FROM competition_formal_exam_realtime_cursor"))
        .isZero();
    assertThat(count("SELECT COUNT(*) FROM competition_formal_exam_outbox")).isZero();
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void headClaimPublishesInSeqOrderAndLeaseRecoveryReclaims() throws Exception {
    LocalDateTime now = LocalDateTime.of(2026, 9, 11, 12, 0, 0);
    for (int seq = 1; seq <= 8; seq++) {
      insertPending(seq, now);
    }
    try (Connection connection = connection()) {
      connection.setAutoCommit(false);
      assertThat(claim(connection, 2, now)).isZero();
      assertThat(claim(connection, 1, now)).isEqualTo(1);
      publish(connection, 1, now);
      connection.commit();
    }
    List<Long> publishedOrder = new ArrayList<>();
    try (Connection connection = connection()) {
      for (int seq = 1; seq <= 8; seq++) {
        if (claim(connection, seq, now) == 1) {
          publish(connection, seq, now);
          publishedOrder.add((long) seq);
        }
      }
    }
    assertThat(publishedOrder).containsExactly(2L, 3L, 4L, 5L, 6L, 7L, 8L);

    insertPending(9, now.minusSeconds(120));
    try (Connection connection = connection(); PreparedStatement update = connection.prepareStatement("""
        UPDATE competition_formal_exam_outbox
        SET status = 'publishing', updated_at = ?
        WHERE seq = 9
        """)) {
      update.setObject(1, now.minusSeconds(120));
      update.executeUpdate();
    }
    int recovered;
    try (Connection connection = connection(); PreparedStatement recover = connection.prepareStatement("""
        UPDATE competition_formal_exam_outbox
        SET status = 'pending', last_error = 'publishing-lease-expired', updated_at = ?
        WHERE status = 'publishing' AND deleted = 0 AND updated_at <= ?
        """)) {
      recover.setObject(1, now);
      recover.setObject(2, now.minusSeconds(30));
      recovered = recover.executeUpdate();
    }
    assertThat(recovered).isEqualTo(1);
    try (Connection connection = connection()) {
      assertThat(claim(connection, 9, now)).isEqualTo(1);
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void duplicateSeqRewriteIsDeterministicBeforeUniqueIndex() throws Exception {
    try (Connection connection = connection(); Statement statement = connection.createStatement()) {
      statement.execute("ALTER TABLE competition_formal_exam_outbox DROP INDEX uk_seq");
    }
    insertPending(1, LocalDateTime.of(2026, 9, 11, 12, 0, 1));
    insertDuplicateSeq(11, 1, LocalDateTime.of(2026, 9, 11, 12, 0, 2));
    try (Connection connection = connection(); Statement statement = connection.createStatement()) {
      statement.execute("""
          UPDATE competition_formal_exam_outbox o
          INNER JOIN (
            SELECT id, ROW_NUMBER() OVER (PARTITION BY tenant_id, exam_session_id ORDER BY created_at, id) AS new_seq
            FROM competition_formal_exam_outbox WHERE deleted = 0
          ) ranked ON ranked.id = o.id
          SET o.seq = ranked.new_seq,
              o.payload_json = JSON_SET(o.payload_json, '$.seq', ranked.new_seq)
          """);
      statement.execute("""
          ALTER TABLE competition_formal_exam_outbox
          ADD UNIQUE KEY uk_seq (tenant_id, exam_session_id, seq, deleted)
          """);
    }
    assertThat(count("SELECT COUNT(*) FROM competition_formal_exam_outbox")).isEqualTo(2);
    assertThat(count("SELECT COUNT(DISTINCT seq) FROM competition_formal_exam_outbox")).isEqualTo(2);
  }

  private void insertOutbox(int index) throws SQLException {
    try (Connection connection = connection()) {
      connection.setAutoCommit(false);
      try {
        long seq = allocate(connection, index);
        try (PreparedStatement insert = connection.prepareStatement("""
            INSERT INTO competition_formal_exam_outbox
              (id, tenant_id, event_id, event_type, aggregate_id, exam_session_id, seq, payload_json,
               status, attempts, created_at, updated_at, deleted)
            VALUES (?, ?, ?, 'candidate.progress.changed', ?, ?, ?, ?, 'pending', 0, NOW(), NOW(), 0)
            """)) {
          insert.setLong(1, 1_000_000L + index);
          insert.setLong(2, TENANT);
          insert.setString(3, "evt-" + index);
          insert.setLong(4, SESSION);
          insert.setLong(5, SESSION);
          insert.setLong(6, seq);
          insert.setString(7, "{\"eventId\":\"evt-" + index + "\",\"seq\":" + seq + "}");
          insert.executeUpdate();
        }
        connection.commit();
      } catch (SQLException ex) {
        connection.rollback();
        throw ex;
      }
    }
  }

  private long allocate(Connection connection, long idSeed) throws SQLException {
    try (PreparedStatement insert = connection.prepareStatement("""
        INSERT INTO competition_formal_exam_realtime_cursor
          (id, tenant_id, exam_session_id, last_seq, deleted, created_at, updated_at)
        VALUES (?, ?, ?, LAST_INSERT_ID(1), 0, NOW(), NOW())
        ON DUPLICATE KEY UPDATE last_seq = LAST_INSERT_ID(last_seq + 1), updated_at = NOW()
        """)) {
      insert.setLong(1, idSeed);
      insert.setLong(2, TENANT);
      insert.setLong(3, SESSION);
      insert.executeUpdate();
    }
    try (Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT LAST_INSERT_ID()")) {
      assertThat(rs.next()).isTrue();
      return rs.getLong(1);
    }
  }

  private void insertPending(int seq, LocalDateTime createdAt) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement insert = connection.prepareStatement("""
            INSERT INTO competition_formal_exam_outbox
              (id, tenant_id, event_id, event_type, aggregate_id, exam_session_id, seq, payload_json,
               status, attempts, created_at, updated_at, deleted)
            VALUES (?, ?, ?, 'session.status.changed', ?, ?, ?, ?, 'pending', 0, ?, ?, 0)
            """)) {
      insert.setLong(1, seq);
      insert.setLong(2, TENANT);
      insert.setString(3, "evt-" + seq);
      insert.setLong(4, SESSION);
      insert.setLong(5, SESSION);
      insert.setLong(6, seq);
      insert.setString(7, "{\"eventId\":\"evt-" + seq + "\",\"seq\":" + seq + "}");
      insert.setObject(8, createdAt);
      insert.setObject(9, createdAt);
      insert.executeUpdate();
    }
  }

  private void insertDuplicateSeq(long id, int seq, LocalDateTime createdAt) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement insert = connection.prepareStatement("""
            INSERT INTO competition_formal_exam_outbox
              (id, tenant_id, event_id, event_type, aggregate_id, exam_session_id, seq, payload_json,
               status, attempts, created_at, updated_at, deleted)
            VALUES (?, ?, ?, 'session.status.changed', ?, ?, ?, ?, 'pending', 0, ?, ?, 0)
            """)) {
      insert.setLong(1, id);
      insert.setLong(2, TENANT);
      insert.setString(3, "dup-" + id);
      insert.setLong(4, SESSION);
      insert.setLong(5, SESSION);
      insert.setInt(6, seq);
      insert.setString(7, "{\"eventId\":\"dup-" + id + "\",\"seq\":" + seq + "}");
      insert.setObject(8, createdAt);
      insert.setObject(9, createdAt);
      insert.executeUpdate();
    }
  }

  private int claim(Connection connection, long id, LocalDateTime now) throws SQLException {
    try (PreparedStatement update = connection.prepareStatement("""
        UPDATE competition_formal_exam_outbox o
        LEFT JOIN competition_formal_exam_outbox prior
          ON prior.tenant_id = o.tenant_id
         AND prior.exam_session_id = o.exam_session_id
         AND prior.deleted = 0
         AND prior.id <> o.id
         AND (
              prior.seq < o.seq
              OR (prior.seq = o.seq AND prior.id < o.id)
             )
         AND prior.status IN ('pending', 'publishing')
        SET o.status = 'publishing',
            o.attempts = o.attempts + 1,
            o.updated_at = ?
        WHERE o.id = ?
          AND o.status = 'pending'
          AND o.deleted = 0
          AND prior.id IS NULL
        """)) {
      update.setObject(1, now);
      update.setLong(2, id);
      return update.executeUpdate();
    }
  }

  private void publish(Connection connection, long id, LocalDateTime now) throws SQLException {
    try (PreparedStatement update = connection.prepareStatement("""
        UPDATE competition_formal_exam_outbox
        SET status = 'published', published_at = ?, updated_at = ?
        WHERE id = ? AND status = 'publishing'
        """)) {
      update.setObject(1, now);
      update.setObject(2, now);
      update.setLong(3, id);
      update.executeUpdate();
    }
  }

  private int count(String sql) throws SQLException {
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
}
