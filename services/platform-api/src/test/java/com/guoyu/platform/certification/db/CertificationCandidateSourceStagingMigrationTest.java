package com.guoyu.platform.certification.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CertificationCandidateSourceStagingMigrationTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V20260610_105__create_certification_candidate_source_staging.sql");

  @Test
  void migrationCreatesCandidateSourceStagingWithWordMappingColumns() throws IOException {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

    assertThat(sql).contains(
        "CREATE TABLE IF NOT EXISTS certification_candidate_source_staging",
        "source_type VARCHAR(40)",
        "external_ref_table VARCHAR(80)",
        "external_ref_id BIGINT",
        "promote_status VARCHAR(40)",
        "raw_payload JSON"
    );

    assertThat(sql).contains(
        "{{candidate_name}}",
        "{{id_card_no}}",
        "{{apply_work_type}}",
        "{{work_resume}}",
        "{{company_code}}",
        "{{work_type_code}}",
        "{{training_summary}}"
    );

    assertThat(sql).contains(
        "Excel:name",
        "Excel:id_card_number",
        "Excel:req_work_type_name",
        "Excel:work_exp",
        "train_sign_up_people_self"
    );
  }
}
