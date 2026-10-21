package com.guoyu.platform.db;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * 空库全量迁移链冒烟（最低验收 #2 / 踩坑库 #34 的修复验证）。
 *
 * <p>410+ 条迁移在"真·空库"上从零跑通才算达标——存量库只应用增量，掩盖不了
 * 链中"假设 legacy 表结构"的断裂点（Duplicate column / 不存在的索引等）。
 *
 * <p>默认不跑（mvn test 跳过）；显式开启：
 * {@code mvn test -Dtest=EmptyDbMigrationChainSmokeTest -DemptyDbSmoke=true}
 * 目标库由环境变量 EMPTY_DB_URL / USER / PASSWORD 指定，默认连本团队测试实例上的
 * 专用验证库 gjjddb_empty_check（每次运行前请先重建为空库）。
 *
 * <p>口径（2026-10-21 修正）：locations 对齐默认 profile 的 application.yml
 * （{@code classpath:db/migration,classpath:db/migration-test}）——共用迁移允许依赖
 * 分叉目录建表（如 competition_practical_sorting_task 由 migration-test 创建），
 * 只跑共用目录不代表任何真实 profile；空库 collation 用 utf8mb4_unicode_ci（对齐测试库
 * gjjddb2026 默认，跨表 JOIN 的迁移在 general_ci 空库上会假性失败）。
 */
@EnabledIfSystemProperty(named = "emptyDbSmoke", matches = "true")
class EmptyDbMigrationChainSmokeTest {

  private static final String DEFAULT_URL = System.getenv().getOrDefault("EMPTY_DB_URL", "");
@Test
  @DisplayName("真空库全量迁移链必须跑通")
  void emptyDatabaseRunsFullMigrationChain() {
    String url = System.getenv().getOrDefault("EMPTY_DB_URL", DEFAULT_URL);
    String user = System.getenv().getOrDefault("EMPTY_DB_USER", "");
    String password = System.getenv().getOrDefault("EMPTY_DB_PASSWORD", "");

    if (url.isBlank() || user.isBlank() || password.isBlank()) {
      throw new IllegalStateException("Set EMPTY_DB_URL, EMPTY_DB_USER, and EMPTY_DB_PASSWORD before enabling this smoke test.");
    }
    Flyway flyway = Flyway.configure()
        .dataSource(url, user, password)
        .locations("classpath:db/migration", "classpath:db/migration-test")
        .baselineOnMigrate(true)
        .outOfOrder(true)
        .validateOnMigrate(false)
        .load();

    assertDoesNotThrow(flyway::migrate);
  }
}
