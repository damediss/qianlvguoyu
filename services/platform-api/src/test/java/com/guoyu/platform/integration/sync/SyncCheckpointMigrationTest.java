package com.guoyu.platform.integration.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** sync_checkpoint 迁移与权限种子契约（与既有迁移测试同一文件内容断言风格） */
class SyncCheckpointMigrationTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V20260805_002__create_sync_checkpoint.sql");

  @Test
  void migrationCreatesSyncCheckpointTable() throws IOException {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);
    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS sync_checkpoint");
    assertThat(sql).contains("PRIMARY KEY (table_name)");
    assertThat(sql).contains("last_max_id");
    assertThat(sql).contains("fail_count");
    assertThat(sql).contains("last_success_at");
  }

  @Test
  void migrationSeedsSyncStatusPermissionForPlatformAdmin() throws IOException {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);
    assertThat(sql).contains("system:sync:view");
    assertThat(sql).contains("INSERT IGNORE INTO sys_permission");
    assertThat(sql).contains("INSERT IGNORE INTO sys_role_permission");
    // 平台管理员 role_id=2000 授权（AGENTS.md 权限三步链）
    assertThat(sql).contains("10001, 1, 2000, 10001");
  }
}
