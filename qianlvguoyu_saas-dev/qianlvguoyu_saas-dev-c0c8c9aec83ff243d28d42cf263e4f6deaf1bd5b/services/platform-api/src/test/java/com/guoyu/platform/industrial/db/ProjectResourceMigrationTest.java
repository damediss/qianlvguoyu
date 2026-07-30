package com.guoyu.platform.industrial.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProjectResourceMigrationTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V20260527_003__seed_industrial_project_resources.sql");

  @Test
  void migrationSeedsProjectMenusAndPermissions() throws IOException {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

    assertThat(sql).contains("开发工程");
    assertThat(sql).contains("/industrial/project/course", "/industrial/project/teach",
        "/industrial/project/exam", "/industrial/project/train", "/industrial/project/criterion");
    assertThat(sql).contains("industrial:project-course:view", "industrial:project-teach:view",
        "industrial:project-exam:view", "industrial:project-train:view",
        "industrial:project-criterion:view");
    assertThat(sql).contains("INSERT IGNORE INTO sys_role_menu");
    assertThat(sql).contains("INSERT IGNORE INTO sys_role_permission");
  }
}
