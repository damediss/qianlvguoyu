package com.guoyu.platform.competition.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Opt-in proof of the FIX-03 row-lock primitive against MySQL 8.4.
 * Run with -Dcompetition.mysql.integration=true; the default unit-test suite remains Docker-free.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "competition.mysql.integration", matches = "true")
class CompetitionScopeConcurrencyMySqlTest {

  @Container
  static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
      .withDatabaseName("competition_test")
      .withUsername("test")
      .withPassword("test");

  @BeforeEach
  void createProbeTable() throws Exception {
    try (Connection connection = connection(); Statement statement = connection.createStatement()) {
      statement.execute("DROP TABLE IF EXISTS competition_scope_lock_probe");
      statement.execute("""
          CREATE TABLE competition_scope_lock_probe (
            id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, member_id BIGINT NOT NULL,
            current_stage_id BIGINT NULL, deleted TINYINT NOT NULL DEFAULT 0,
            UNIQUE KEY uk_scope_lock_probe (tenant_id, member_id, deleted)
          ) ENGINE=InnoDB
          """);
      statement.execute("""
          INSERT INTO competition_scope_lock_probe
            (id, tenant_id, member_id, current_stage_id, deleted)
          VALUES (1, 7, 8, NULL, 0)
          """);
    }
  }

  @Test
  void stageUpdateWaitsForProfileForUpdateAndThenSeesCommittedValue() throws Exception {
    try (Connection locker = connection(); Connection updater = connection()) {
      locker.setAutoCommit(false);
      updater.setAutoCommit(false);
      try (PreparedStatement lock = locker.prepareStatement("""
          SELECT id FROM competition_scope_lock_probe
          WHERE tenant_id = ? AND member_id = ? AND deleted = 0 FOR UPDATE
          """)) {
        lock.setLong(1, 7L);
        lock.setLong(2, 8L);
        try (ResultSet ignored = lock.executeQuery()) {
          assertThat(ignored.next()).isTrue();
        }
      }

      CompletableFuture<Boolean> updateFinished = CompletableFuture.supplyAsync(() -> {
        try (PreparedStatement update = updater.prepareStatement("""
            UPDATE competition_scope_lock_probe SET current_stage_id = 99
            WHERE tenant_id = 7 AND member_id = 8 AND deleted = 0
            """)) {
          update.executeUpdate();
          updater.commit();
          return true;
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      });
      Thread.sleep(150L);
      assertThat(updateFinished).isNotCompleted();
      locker.commit();
      assertThat(updateFinished.get(5, TimeUnit.SECONDS)).isTrue();

      try (Statement statement = updater.createStatement();
          ResultSet result = statement.executeQuery(
              "SELECT current_stage_id FROM competition_scope_lock_probe WHERE id = 1")) {
        assertThat(result.next()).isTrue();
        assertThat(result.getLong(1)).isEqualTo(99L);
      }
    }
  }

  private Connection connection() throws Exception {
    return DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
  }
}
