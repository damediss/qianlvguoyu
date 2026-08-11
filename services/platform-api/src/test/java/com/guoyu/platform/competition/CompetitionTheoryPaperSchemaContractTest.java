package com.guoyu.platform.competition;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryPaperQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryRequirementEntity;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionTheoryPaperSchemaContractTest {

  private static final List<Class<? extends BaseEntity>> ENTITIES = List.of(
      CompetitionTheoryRequirementEntity.class,
      CompetitionTheoryPaperSnapshotEntity.class,
      CompetitionTheoryPaperQuestionEntity.class);

  @Test
  void everyBusinessTableHasCompleteBaseEntityColumns() throws IOException {
    String migration = migration();
    for (Class<? extends BaseEntity> entity : ENTITIES) {
      String table = entity.getAnnotation(TableName.class).value();
      assertThat(tableDdl(migration, table))
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
            .as(table + "." + field.getName()).isTrue();
      }
    }
  }

  @Test
  void migrationFreezesOnlyObjectiveQuestionsAndCurrentPublishedVersion() throws IOException {
    String migration = migration();

    assertThat(migration.toUpperCase()).doesNotContain(" FLOAT", " DOUBLE");
    assertThat(migration)
        .contains("question_type IN ('single', 'multi', 'judge')")
        .contains("options_json JSON NOT NULL")
        .contains("correct_answers_json JSON NOT NULL")
        .contains("scoring_rule_code VARCHAR(64) NOT NULL DEFAULT 'exact_match'")
        .contains("active_published_key TINYINT GENERATED ALWAYS AS")
        .contains("UNIQUE KEY uk_comp_theory_snapshot_current")
        .contains("'competition:theory-paper:view'")
        .contains("'competition:theory-paper:manage'")
        .contains("AND rp.role_id = 2000");
  }

  @Test
  void coursePaperSnapshotsDoNotRequireCompetitionTheoryRequirement() throws IOException {
    assertThat(migration()).contains("MODIFY COLUMN requirement_id BIGINT NULL");
  }

  @Test
  void coursePaperSnapshotsDoNotRequireCompetitionScoreRule() throws IOException {
    assertThat(migration()).contains("MODIFY COLUMN score_rule_id BIGINT NULL");
  }

  private String migration() throws IOException {
    try (var input = getClass().getClassLoader().getResourceAsStream(
        "db/migration/V20260801_002__create_competition_theory_paper_snapshots.sql");
         var additiveInput = getClass().getClassLoader().getResourceAsStream(
             "db/migration/V20260802_003__extend_competition_scoring_to_levels.sql");
         var decouplingInput = getClass().getClassLoader().getResourceAsStream(
             "db/migration/V20260810_003__allow_competition_theory_paper_import_without_score_rule.sql")) {
      assertThat(input).isNotNull();
      assertThat(additiveInput).isNotNull();
      assertThat(decouplingInput).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8)
          + "\n" + new String(additiveInput.readAllBytes(), StandardCharsets.UTF_8)
          + "\n" + new String(decouplingInput.readAllBytes(), StandardCharsets.UTF_8);
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
