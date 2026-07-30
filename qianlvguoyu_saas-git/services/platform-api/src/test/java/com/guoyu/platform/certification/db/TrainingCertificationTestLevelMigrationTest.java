package com.guoyu.platform.certification.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TrainingCertificationTestLevelMigrationTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V20260611_005__repair_training_certification_test_levels.sql");

  @Test
  void migrationAlignsTrainingTestDataWithJuniorCertificationTask() throws IOException {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

    assertThat(sql).contains(
        "SET work_type_level = 5",
        "SET req_level = 0",
        "level = '0'",
        "work_type_paper_level = 0",
        "id BETWEEN 91011 AND 91015"
    );
  }
}
