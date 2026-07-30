package com.guoyu.platform.industrial.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class IndustrialPlatformMigrationTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V20260527_002__create_industrial_platform_items.sql");

  @Test
  void migrationCreatesBusinessTableWithTenantAndSoftDeleteColumns() throws IOException {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS industrial_platform_item");
    assertThat(sql).contains("id BIGINT NOT NULL");
    assertThat(sql).doesNotContain("AUTO_INCREMENT");
    assertThat(sql).contains("tenant_id BIGINT NOT NULL DEFAULT 1");
    assertThat(sql).contains("category VARCHAR(40) NOT NULL");
    assertThat(sql).contains("company VARCHAR(200) NOT NULL");
    assertThat(sql).contains("name VARCHAR(200) NOT NULL");
    assertThat(sql).contains("nums INT NULL");
    assertThat(sql).contains("item_date DATE NULL");
    assertThat(sql).contains("state VARCHAR(80) NULL");
    assertThat(sql).contains("deleted INT NOT NULL DEFAULT 0");
  }

  @Test
  void migrationSeedsPlatformConstructionMenusAndPermissions() throws IOException {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

    assertThat(sql).contains("平台建设");
    assertThat(sql).contains("技能大师工作室", "学习型班组", "竞赛", "新型学徒", "自主评价");
    assertThat(sql).contains("/industrial/platform-master", "/industrial/platform-study",
        "/industrial/platform-match", "/industrial/platform-apprentice", "/industrial/platform-comment");
    assertThat(sql).contains("industrial:platform-master:view", "industrial:platform-study:create",
        "industrial:platform-match:update", "industrial:platform-apprentice:delete",
        "industrial:platform-comment:view");
  }
}
