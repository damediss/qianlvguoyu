package com.guoyu.platform.competition;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryAssignmentAdjustmentEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryCandidateAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryExamRoomEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryExamSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryScheduleVersionEntity;
import com.guoyu.platform.competition.entity.CompetitionTheorySessionRoomEntity;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionTheorySchedulingSchemaContractTest {

  private static final List<Class<? extends BaseEntity>> ENTITIES = List.of(
      CompetitionTheoryScheduleVersionEntity.class,
      CompetitionTheoryExamRoomEntity.class,
      CompetitionTheoryExamSessionEntity.class,
      CompetitionTheorySessionRoomEntity.class,
      CompetitionTheoryCandidateAssignmentEntity.class,
      CompetitionTheoryAssignmentAdjustmentEntity.class);

  @Test
  void everySchedulingTableHasCompleteBaseEntityColumnsAndMappedFields() throws IOException {
    String migration = migration();
    for (Class<? extends BaseEntity> entity : ENTITIES) {
      String table = entity.getAnnotation(TableName.class).value();
      String ddl = tableDdl(migration, table);
      assertThat(ddl).as(table).contains(
          "id BIGINT NOT NULL PRIMARY KEY",
          "tenant_id BIGINT NOT NULL",
          "created_by BIGINT NULL",
          "updated_by BIGINT NULL",
          "created_at DATETIME NOT NULL",
          "updated_at DATETIME NOT NULL",
          "deleted TINYINT NOT NULL DEFAULT 0");
      for (Field field : entity.getDeclaredFields()) {
        String column = snakeCase(field.getName());
        assertThat(ddl.contains("\n  " + column + " ")
            || migration.contains("ADD COLUMN " + column + " "))
            .as(table + "." + field.getName()).isTrue();
      }
    }
  }

  @Test
  void migrationDefinesVersionLocksConflictIndexesAndSchedulePermissions() throws IOException {
    String migration = migration();
    assertThat(migration)
        .contains("status IN ('draft', 'published', 'withdrawn')")
        .contains("UNIQUE KEY uk_comp_theory_schedule_active")
        .contains("UNIQUE KEY uk_comp_theory_schedule_published")
        .contains("UNIQUE KEY uk_comp_theory_assignment_current")
        .contains("active_record_key TINYINT GENERATED ALWAYS AS")
        .contains("competition:theory-schedule:view")
        .contains("competition:theory-schedule:manage")
        .contains("competition:theory-schedule:publish")
        .contains("competition:theory-roster:export")
        .contains("theory_schedule_published")
        .contains("theory_schedule_withdrawn");
  }

  @Test
  void rosterModelAndMigrationDoNotAddSensitiveIdentityFields() throws IOException {
    String migration = migration().toLowerCase();
    String schedulingTables = migration.substring(
        migration.indexOf("create table competition_theory_schedule_version"),
        migration.indexOf("alter table competition_player_message"));
    assertThat(schedulingTables)
        .doesNotContain("mobile", "phone", "id_card", "identity_no", "correct_answers");
  }

  private String migration() throws IOException {
    try (var input = getClass().getClassLoader().getResourceAsStream(
        "db/migration/V20260801_003__create_competition_theory_scheduling.sql");
         var executionInput = getClass().getClassLoader().getResourceAsStream(
             "db/migration/V20260801_004__create_competition_theory_exam_execution.sql");
         var additiveInput = getClass().getClassLoader().getResourceAsStream(
             "db/migration/V20260802_003__extend_competition_scoring_to_levels.sql")) {
      assertThat(input).isNotNull();
      assertThat(executionInput).isNotNull();
      assertThat(additiveInput).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8)
          + "\n" + new String(executionInput.readAllBytes(), StandardCharsets.UTF_8)
          + "\n" + new String(additiveInput.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private String tableDdl(String migration, String table) {
    int start = migration.indexOf("CREATE TABLE " + table + " (");
    assertThat(start).as(table + " create statement").isGreaterThanOrEqualTo(0);
    int end = migration.indexOf(") ENGINE=InnoDB", start);
    assertThat(end).as(table + " engine clause").isGreaterThan(start);
    return migration.substring(start, end);
  }

  private String snakeCase(String value) {
    return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
  }
}
