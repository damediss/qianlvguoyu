package com.guoyu.platform.certification.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CertificationPracticalScoringMigrationTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V20260920_105__widen_certification_scoring_element.sql");

  @Test
  void migrationStoresImportedScoringElementsAsText() throws IOException {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

    assertThat(sql)
        .contains("ALTER TABLE certification_assessor_task_score")
        .contains("MODIFY COLUMN step_name TEXT NOT NULL")
        .contains("COMMENT '评分要素'")
        .doesNotContain("MODIFY COLUMN step_name VARCHAR(200)");
  }
}
