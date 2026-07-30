package com.guoyu.platform.certification.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CertificationInfoMigrationTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V20260602_006__create_certification_info_management.sql");

  private static final Path MENU_REPAIR_MIGRATION = Path.of(
      "src/main/resources/db/migration/V20260602_008__repair_certification_menu_tree.sql");

  private static final Path EXAM_STAFF_MIGRATION = Path.of(
      "src/main/resources/db/migration/V20260603_010__create_certification_info_exam_staff.sql");

  private static final Path REMOVE_PRE_EXAM_MIGRATION = Path.of(
      "src/main/resources/db/migration/V20260605_005__remove_pre_exam_feature.sql");

  @Test
  void migrationCreatesInfoManagementTablesAndExtendsExistingMasterData() throws IOException {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

    assertThat(sql).contains(
        "ALTER TABLE certification_assessor",
        "ADD COLUMN audit_status",
        "ADD COLUMN work_status",
        "ALTER TABLE certification_exam_site",
        "CREATE TABLE IF NOT EXISTS certification_exam_equipment",
        "CREATE TABLE IF NOT EXISTS certification_info_audit_record",
        "tenant_id BIGINT NOT NULL DEFAULT 1",
        "created_by BIGINT NULL",
        "updated_by BIGINT NULL",
        "deleted INT NOT NULL DEFAULT 0",
        "UNIQUE KEY uk_cert_exam_equipment_asset"
    );
  }

  @Test
  void migrationSeedsDynamicMenusAndPermissions() throws IOException {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

    assertThat(sql).contains(
        "信息管理",
        "考评员信息",
        "考场信息",
        "考试设备",
        "certification/info/AssessorInfoPage",
        "certification.info.assessor.view",
        "certification.info.assessor.create",
        "certification.info.assessor.submit",
        "certification.info.assessor.review",
        "certification.info.site.view",
        "certification.info.site.create",
        "certification.info.site.submit",
        "certification.info.site.review",
        "certification.info.equipment.view",
        "certification.info.equipment.create",
        "certification.info.equipment.submit",
        "certification.info.equipment.review"
    );
  }

  @Test
  void migrationKeepsInfoManagementAsIndependentCertificationModule() throws IOException {
    String sql = Files.readString(MENU_REPAIR_MIGRATION, StandardCharsets.UTF_8);

    assertThat(sql).contains(
        "(6330, 1, 6300, 'directory', 'certification_info', '信息管理'",
        "parent_id = 6330",
        "code IN ('certification_info_assessor', 'certification_info_site', 'certification_info_equipment')",
        "(806330, 1, 2000, 6330",
        "(999104, 1, 2604, 6330"
    );
    assertThat(sql).doesNotContain(
        "(6310, 1, 6300, 'directory', 'certification_info'",
        "(6311, 1, 6310, 'page', 'certification_info_assessor'",
        "(6312, 1, 6310, 'page', 'certification_info_site'",
        "(6313, 1, 6310, 'page', 'certification_info_equipment'"
    );
  }

  @Test
  void examStaffMigrationCreatesTableMenusAndPermissions() throws IOException {
    String sql = Files.readString(EXAM_STAFF_MIGRATION, StandardCharsets.UTF_8);

    assertThat(sql).contains(
        "CREATE TABLE IF NOT EXISTS certification_info_exam_staff",
        "考务信息",
        "certification_info_exam_staff",
        "certification/info/ExamStaffInfoPage",
        "certification.info.exam-staff.view",
        "certification.info.exam-staff.create",
        "certification.info.exam-staff.submit",
        "certification.info.exam-staff.review"
    );
  }

  @Test
  void removePreExamMigrationDisablesMenusAndPermissions() throws IOException {
    String sql = Files.readString(REMOVE_PRE_EXAM_MIGRATION, StandardCharsets.UTF_8);

    assertThat(sql).contains(
        "m.code = 'certification_pre_exam'",
        "p.code LIKE 'certification.pre_exam.%'",
        "visible = 0",
        "status = 'disabled'",
        "deleted = 1"
    );
  }
}
