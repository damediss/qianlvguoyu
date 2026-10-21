package com.guoyu.platform.db;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * 单迁移定点空库验证：共用链跑到 ai_review_answer 建表之后、新迁移之前，再由外层脚本
 * 直接执行目标迁移 SQL 两次（语法 + 幂等守卫）。用于绕开存量 20260924.001 双目录撞号
 * （owner 级处置，见 AGENTS.md 数据库铁律 2）对全链冒烟的阻塞。
 */
@EnabledIfSystemProperty(named = "singleMigrationSmoke", matches = "true")
class SingleMigrationEmptyDbSmokeTest {

  @Test
  @DisplayName("共用链定点：应用至 20261008.003 为止（含 ai_review_answer 建表）")
  void applyChainUpToGradeSourcePredecessor() {
    String url = System.getenv("EMPTY_DB_URL");
    String user = System.getenv("EMPTY_DB_USER");
    String password = System.getenv("EMPTY_DB_PASSWORD");

    Flyway flyway = Flyway.configure()
        .dataSource(url, user, password)
        .locations("classpath:db/migration")
        .target("20261008.003")
        .baselineOnMigrate(true)
        .outOfOrder(true)
        .validateOnMigrate(false)
        .load();

    assertDoesNotThrow(flyway::migrate);
  }
}
