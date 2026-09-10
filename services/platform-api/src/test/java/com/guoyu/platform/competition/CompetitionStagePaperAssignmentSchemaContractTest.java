package com.guoyu.platform.competition;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import com.guoyu.platform.competition.entity.CompetitionStagePaperAssignmentEntity;
import com.guoyu.platform.competition.mapper.CompetitionStagePaperAssignmentMapper;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CompetitionStagePaperAssignmentSchemaContractTest {

  @Test
  void assignmentEntityAndMigrationContainTheCompleteBaseContract() throws IOException {
    String migration = migration();
    String table = CompetitionStagePaperAssignmentEntity.class
        .getAnnotation(TableName.class).value();
    String ddl = tableDdl(migration, table);
    assertThat(CompetitionStagePaperAssignmentEntity.class.getSuperclass())
        .isEqualTo(BaseEntity.class);
    assertThat(ddl).contains(
        "id BIGINT NOT NULL PRIMARY KEY",
        "tenant_id BIGINT NOT NULL",
        "paper_snapshot_id BIGINT NULL",
        "created_by BIGINT NULL",
        "updated_by BIGINT NULL",
        "created_at DATETIME NOT NULL",
        "updated_at DATETIME NOT NULL",
        "deleted TINYINT NOT NULL DEFAULT 0",
        "UNIQUE KEY uk_comp_stage_paper_assignment_contestant");
    for (Field field : CompetitionStagePaperAssignmentEntity.class.getDeclaredFields()) {
      assertThat(ddl.contains("\n  " + snakeCase(field.getName()) + " "))
          .as(table + "." + field.getName()).isTrue();
    }
  }

  @Test
  void eligiblePlayerQueryIncludesImportedAndUnactivatedContestants() {
    assertThat(CompetitionStagePaperAssignmentMapper.ELIGIBLE_PLAYERS_SQL)
        .contains("eligible_unactivated")
        .contains("eligible_activated")
        .contains("imported_pending_confirm")
        .contains("pending_audit")
        .contains("LEFT JOIN competition_event_work_type wt")
        .contains("OR p.event_work_type_id IS NULL")
        .contains("OR p.event_work_type_level_id IS NULL");
  }

  @Test
  void migrationKeepsAssignmentIndependentFromExamSessionsAndSeedsPermissions() throws IOException {
    String migration = migration();
    assertThat(migration)
        .contains("allocation_source IN ('random', 'manual')")
        .contains("status IN ('draft', 'confirmed')")
        .contains("competition:stage-paper-assignment:view")
        .contains("competition:stage-paper-assignment:manage")
        .contains("competition:stage-paper-assignment:confirm")
        .doesNotContain("session_id BIGINT NOT NULL")
        .doesNotContain("room_id BIGINT NOT NULL");
  }

  private String migration() throws IOException {
    try (var input = getClass().getClassLoader().getResourceAsStream(
        "db/migration/V20260907_004__create_competition_stage_paper_assignments.sql")) {
      assertThat(input).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private String tableDdl(String migration, String table) {
    String lower = migration.toLowerCase();
    int start = lower.indexOf("create table " + table + " (");
    assertThat(start).as(table).isGreaterThanOrEqualTo(0);
    int end = lower.indexOf(") engine=innodb", start);
    assertThat(end).as(table).isGreaterThan(start);
    return migration.substring(start, end);
  }

  private String snakeCase(String value) {
    return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
  }
}
