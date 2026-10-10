package com.guoyu.platform.certification.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CertificationPreparationMigrationTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V20260530_001__create_certification_preparation_tables.sql");

  @Test
  void migrationCreatesPreparationTablesWithAuditAndTenantColumns() throws IOException {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

    assertThat(sql).contains(
        "CREATE TABLE IF NOT EXISTS certification_candidate_import_batch",
        "CREATE TABLE IF NOT EXISTS certification_candidate_import_error",
        "CREATE TABLE IF NOT EXISTS certification_candidate",
        "CREATE TABLE IF NOT EXISTS certification_material_template",
        "CREATE TABLE IF NOT EXISTS certification_candidate_document",
        "CREATE TABLE IF NOT EXISTS certification_application_material_batch",
        "CREATE TABLE IF NOT EXISTS certification_application_material_item",
        "CREATE TABLE IF NOT EXISTS certification_exam_site",
        "CREATE TABLE IF NOT EXISTS certification_task_exam_site",
        "CREATE TABLE IF NOT EXISTS certification_assessor",
        "CREATE TABLE IF NOT EXISTS certification_task_assessor_group",
        "CREATE TABLE IF NOT EXISTS certification_task_assessor",
        "CREATE TABLE IF NOT EXISTS certification_assessor_task",
        "CREATE TABLE IF NOT EXISTS certification_assessor_task_score",
        "CREATE TABLE IF NOT EXISTS certification_assessor_notification",
        "tenant_id BIGINT NOT NULL DEFAULT 1",
        "created_by BIGINT NULL",
        "updated_by BIGINT NULL",
        "deleted INT NOT NULL DEFAULT 0"
    );
  }

  @Test
  void migrationSeedsMaterialTemplatesMenuAndPermissions() throws IOException {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

    assertThat(sql).contains("DOC001", "DOC002", "DOC003", "DOC004", "DOC005");
    assertThat(sql).contains("DOC006", "DOC007", "DOC008", "DOC009", "DOC010");
    assertThat(sql).contains("认定筹备", "/certification/preparation", "certification/Preparation");
    assertThat(sql).contains(
        "certification.preparation.task.view",
        "certification.preparation.candidate.import",
        "certification.preparation.document.review",
        "certification.preparation.material.generate",
        "certification.preparation.site.assign",
        "certification.preparation.assessor.dispatch",
        "certification.preparation.assessor.score",
        "certification.preparation.assessor.review",
        "certification.preparation.complete"
    );
  }
}
