package com.guoyu.platform.certification.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CertificationProjectMigrationTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V20260529_001__create_certification_project.sql");
  private static final Path SCOPE_MIGRATION = Path.of(
      "src/main/resources/db/migration/V20260601_002__add_certification_task_scope.sql");
  private static final Path WORKFLOW_MIGRATION = Path.of(
      "src/main/resources/db/migration/V20260602_002__create_certification_document_workflow_record.sql");
  private static final Path WORKFLOW_PERMISSION_REPAIR_MIGRATION = Path.of(
      "src/main/resources/db/migration/V20260602_003__repair_certification_document_workflow_admin_permissions.sql");

  @Test
  void migrationCreatesCertificationProjectTablesWithAuditColumns() throws IOException {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);
    String scopeSql = Files.readString(SCOPE_MIGRATION, StandardCharsets.UTF_8);

    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS certification_task");
    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS certification_task_document");
    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS certification_task_member");
    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS certification_wbs_item");
    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS certification_schedule_item");
    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS certification_risk");
    assertThat(scopeSql).contains("CREATE TABLE IF NOT EXISTS certification_task_scope");
    assertThat(Files.readString(WORKFLOW_MIGRATION, StandardCharsets.UTF_8))
        .contains("CREATE TABLE IF NOT EXISTS certification_document_workflow_record");
    assertThat(sql).contains("tenant_id BIGINT NOT NULL DEFAULT 1");
    assertThat(sql).contains("created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
    assertThat(sql).contains("updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
    assertThat(sql).contains("deleted INT NOT NULL DEFAULT 0");
  }

  @Test
  void migrationSeedsCertificationMenuAndFineGrainedPermissions() throws IOException {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

    assertThat(sql).contains("认定管理", "项目管理");
    assertThat(sql).contains("/certification/project");
    assertThat(sql).contains("certification.project.task.list");
    assertThat(sql).contains("certification.project.task.create");
    assertThat(sql).contains("certification.project.members.edit");
    assertThat(sql).contains("certification.project.wbs.submit");
    assertThat(sql).contains("certification.project.schedule.submit");
    assertThat(sql).contains("certification.project.risk.submit");
    assertThat(Files.readString(WORKFLOW_MIGRATION, StandardCharsets.UTF_8))
        .contains(
            "certification.project.document.submit",
            "certification.project.document.initialReview",
            "certification.project.document.finalReview",
            "certification.project.document.workflowView");
  }

  @Test
  void workflowPermissionsAreGrantedToCertificationAdminWithUniqueIds() throws IOException {
    String workflowSql = Files.readString(WORKFLOW_MIGRATION, StandardCharsets.UTF_8);
    String repairSql = Files.readString(WORKFLOW_PERMISSION_REPAIR_MIGRATION, StandardCharsets.UTF_8);

    assertThat(workflowSql).contains(
        "(6944, 1, 6301, 'certification.project.document.submit'",
        "(6945, 1, 6301, 'certification.project.document.initialReview'",
        "(6946, 1, 6301, 'certification.project.document.finalReview'",
        "(6947, 1, 6301, 'certification.project.document.workflowView'");
    assertThat(workflowSql).doesNotContain(
        "(6921, 1, 6301, 'certification.project.document.submit'",
        "(6922, 1, 6301, 'certification.project.document.initialReview'",
        "(6923, 1, 6301, 'certification.project.document.finalReview'",
        "(6924, 1, 6301, 'certification.project.document.workflowView'");
    assertThat(workflowSql).doesNotContain(
        "2000, 6944",
        "2000, 6945",
        "2000, 6946",
        "2000, 6947");
    assertThat(repairSql).contains(
        "2604, 1, 'CERTIFICATION_ADMIN'",
        "1011, 1, 100, 'certification_admin'",
        "'CERTIFICATION_ADMIN'",
        "认定管理员",
        "3011, 1, 1011, 2604",
        "907100 + p.id - 6900",
        "sys_user",
        "sys_role",
        "sys_user_role",
        "sys_role_menu",
        "sys_role_permission",
        "certification.project.task.list",
        "certification.project.task.create",
        "certification.preparation.task.view",
        "certification.project.document.submit",
        "certification.project.document.initialReview",
        "certification.project.document.finalReview",
        "certification.project.document.workflowView");
    assertThat(repairSql).doesNotContain(
        "'cert_admin'",
        "'PLATFORM_ADMIN'",
        "2004, 1, 'CERTIFICATION_ADMIN'",
        "3004, 1, 1010, 2004",
        "906950 + p.id - 6900");
  }
}
