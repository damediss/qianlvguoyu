package com.guoyu.platform.certification.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CertificationPreparationExamStaffMigrationTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V20260603_012__create_certification_task_exam_staff.sql");

  @Test
  void migrationCreatesTaskExamStaffTableAndPermissions() throws IOException {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

    assertThat(sql).contains(
        "CREATE TABLE IF NOT EXISTS certification_task_exam_staff",
        "certification.preparation.staff.view",
        "certification.preparation.staff.assign"
    );
  }
}
