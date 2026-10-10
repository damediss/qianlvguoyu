package com.guoyu.platform.certification.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CertificationCandidateDocumentReductionMigrationTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V20260602_007__reduce_candidate_documents_to_two.sql");

  @Test
  void migrationRetainsOnlyRegistrationFormAndPhotoTemplates() throws IOException {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

    assertThat(sql).contains("DOC001", "DOC002");
    assertThat(sql).contains("报名信息表", "两寸证件照");
    assertThat(sql).contains("item_code NOT IN ('DOC001', 'DOC002')");
    assertThat(sql).contains("doc_code NOT IN ('DOC001', 'DOC002')");
    assertThat(sql).contains("document_required_count = 2");
    assertThat(sql).doesNotContain("DOC003");
  }
}
