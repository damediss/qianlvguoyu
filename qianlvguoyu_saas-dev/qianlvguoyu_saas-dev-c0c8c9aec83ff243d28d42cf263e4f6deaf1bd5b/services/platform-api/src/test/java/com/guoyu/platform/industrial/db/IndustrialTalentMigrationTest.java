package com.guoyu.platform.industrial.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class IndustrialTalentMigrationTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V20260527_004__create_industrial_talent.sql");

  @Test
  void migrationCreatesTalentTablesWithTenantAndSoftDeleteColumns() throws IOException {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS industrial_talent_training");
    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS industrial_talent_service");
    assertThat(sql).contains("tenant_id BIGINT NOT NULL DEFAULT 1");
    assertThat(sql).contains("level VARCHAR(80) NOT NULL");
    assertThat(sql).contains("work_type VARCHAR(120) NOT NULL");
    assertThat(sql).contains("service_date VARCHAR(40) NOT NULL");
    assertThat(sql).doesNotContain("AUTO_INCREMENT");
    assertThat(sql).contains("deleted INT NOT NULL DEFAULT 0");
  }

  @Test
  void migrationSeedsTalentMenusPermissionsAndDemoRows() throws IOException {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

    assertThat(sql).contains("人才培养", "人才服务");
    assertThat(sql).contains("/industrial/talent/training", "/industrial/talent/service");
    assertThat(sql).contains("industrial:talent-training:view", "industrial:talent-service:view");
    assertThat(sql).contains("千铝培训", "高级工", "电解槽操作工");
    assertThat(sql).contains("2025.8.5技师认定交流会", "高级技师论文职称指导");
    assertThat(sql).contains("INSERT IGNORE INTO sys_role_menu");
    assertThat(sql).contains("INSERT IGNORE INTO sys_role_permission");
  }
}
