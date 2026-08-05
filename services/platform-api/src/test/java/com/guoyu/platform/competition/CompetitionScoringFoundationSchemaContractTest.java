package com.guoyu.platform.competition;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreEntity;
import com.guoyu.platform.competition.entity.CompetitionRankingEntryEntity;
import com.guoyu.platform.competition.entity.CompetitionRankingSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeAvoidanceEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreUnlockRequestEntity;
import com.guoyu.platform.competition.entity.CompetitionTotalScoreSnapshotEntity;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionScoringFoundationSchemaContractTest {

  private static final List<Class<? extends BaseEntity>> ENTITIES = List.of(
      CompetitionScoreSourceEntity.class,
      CompetitionRefereeAssignmentEntity.class,
      CompetitionRefereeAvoidanceEntity.class,
      CompetitionPracticalScoreEntity.class,
      CompetitionScoreUnlockRequestEntity.class,
      CompetitionTotalScoreSnapshotEntity.class,
      CompetitionRankingSnapshotEntity.class,
      CompetitionRankingEntryEntity.class);

  @Test
  void everyBusinessTableHasCompleteBaseEntityColumns() throws IOException {
    String migration = migration();
    for (Class<? extends BaseEntity> entity : ENTITIES) {
      String table = entity.getAnnotation(TableName.class).value();
      String ddl = tableDdl(migration, table);
      assertThat(ddl)
          .as(table)
          .contains(
              "id BIGINT NOT NULL PRIMARY KEY",
              "tenant_id BIGINT NOT NULL",
              "created_by BIGINT NULL",
              "updated_by BIGINT NULL",
              "created_at DATETIME NOT NULL",
              "updated_at DATETIME NOT NULL",
              "deleted TINYINT NOT NULL DEFAULT 0");
    }
  }

  @Test
  void everyEntityFieldMapsToSnakeCaseDdlColumn() throws IOException {
    String migration = migration();
    for (Class<? extends BaseEntity> entity : ENTITIES) {
      String table = entity.getAnnotation(TableName.class).value();
      String ddl = tableDdl(migration, table);
      for (Field field : entity.getDeclaredFields()) {
        String column = snakeCase(field.getName());
        assertThat(ddl.contains("\n  " + column + " ")
            || migration.toLowerCase().contains("add column " + column + " "))
            .as(table + "." + field.getName())
            .isTrue();
      }
    }
  }

  @Test
  void migrationUsesDecimalsAndDefinesVersionCurrentAndSnapshotInvariants()
      throws IOException {
    String migration = migration();

    assertThat(migration.toUpperCase()).doesNotContain(" FLOAT", " DOUBLE");
    assertThat(migration)
        .contains("raw_score DECIMAL(12,6)")
        .contains("total_score DECIMAL(12,6)")
        .contains("active_current_key TINYINT GENERATED ALWAYS AS")
        .contains("UNIQUE KEY uk_comp_score_source_current")
        .contains("UNIQUE KEY uk_comp_practical_score_current")
        .contains("UNIQUE KEY uk_comp_total_score_current")
        .contains("UNIQUE KEY uk_comp_ranking_snapshot_version")
        .contains("UNIQUE KEY uk_comp_ranking_entry_contestant")
        .contains("source_type IN ('theory', 'simulation', 'practical')")
        .contains("status IN ('pending', 'approved', 'rejected')")
        .contains("contestant_status = 'absent' AND display_rank IS NULL AND total_score = 0");
  }

  private String migration() throws IOException {
    try (var input = getClass().getClassLoader().getResourceAsStream(
        "db/migration/V20260727_006__create_competition_scoring_foundation.sql");
         var additiveInput = getClass().getClassLoader().getResourceAsStream(
             "db/migration/V20260802_003__extend_competition_scoring_to_levels.sql")) {
      assertThat(input).isNotNull();
      assertThat(additiveInput).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8)
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
