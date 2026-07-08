package com.guoyu.platform.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

  private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

  /**
   * 启动前先清理 Flyway 历史脏数据，再 repair + migrate。
   * <p>
   * 典型场景：历史上 JDBC 迁移脚本改名/删除后，repair 多次执行会在
   * {@code flyway_schema_history} 中为同一 version 写入多条 DELETE 记录，
   * 导致 Flyway 报 {@code Corrupted schema history} 并阻断启动。
   */
  @Bean
  public FlywayMigrationStrategy flywayMigrationStrategy(DataSource dataSource) {
    return flyway -> {
      sanitizeSchemaHistory(dataSource);
      flyway.repair();
      sanitizeSchemaHistory(dataSource);
      flyway.migrate();
    };
  }

  private void sanitizeSchemaHistory(DataSource dataSource) {
    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement()) {
      int removedJdbcOrphans = statement.executeUpdate("""
          DELETE FROM flyway_schema_history
          WHERE type = 'JDBC'
            AND version IN (
              SELECT version FROM (
                SELECT version
                FROM flyway_schema_history
                WHERE type = 'SQL'
                  AND success = 1
              ) applied_sql_versions
            )
          """);
      if (removedJdbcOrphans > 0) {
        log.warn("Removed {} orphan JDBC Flyway history rows before repair/migrate", removedJdbcOrphans);
      }

      int removedConflictingDeletes = statement.executeUpdate("""
          DELETE FROM flyway_schema_history
          WHERE type = 'DELETE'
            AND version IN (
              SELECT version FROM (
                SELECT version
                FROM flyway_schema_history
                WHERE type = 'SQL'
                  AND success = 1
              ) applied_sql_versions
            )
          """);
      if (removedConflictingDeletes > 0) {
        log.warn("Removed {} conflicting Flyway DELETE rows for already-applied SQL versions", removedConflictingDeletes);
      }

      int removedDuplicateSql = statement.executeUpdate("""
          DELETE f1
          FROM flyway_schema_history f1
          INNER JOIN (
            SELECT version, MIN(installed_rank) AS keep_rank
            FROM flyway_schema_history
            WHERE type = 'SQL'
              AND success = 1
            GROUP BY version
            HAVING COUNT(*) > 1
          ) keepers ON f1.version = keepers.version
          WHERE f1.type = 'SQL'
            AND f1.success = 1
            AND f1.installed_rank <> keepers.keep_rank
          """);
      if (removedDuplicateSql > 0) {
        log.warn("Removed {} duplicate successful SQL Flyway history rows", removedDuplicateSql);
      }

      int removedDuplicateDeletes = statement.executeUpdate("""
          DELETE f1
          FROM flyway_schema_history f1
          INNER JOIN (
            SELECT version, MAX(installed_rank) AS keep_rank
            FROM flyway_schema_history
            WHERE type = 'DELETE'
            GROUP BY version
            HAVING COUNT(*) > 1
          ) keepers ON f1.version = keepers.version
          WHERE f1.type = 'DELETE'
            AND f1.installed_rank <> keepers.keep_rank
          """);
      if (removedDuplicateDeletes > 0) {
        log.warn("Removed {} duplicate Flyway DELETE history rows before repair/migrate", removedDuplicateDeletes);
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to sanitize Flyway schema history", ex);
    }
  }
}
