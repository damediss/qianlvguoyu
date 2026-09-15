package com.guoyu.platform.competition;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalScoringTaskEntity;
import com.guoyu.platform.competition.entity.CompetitionSimulationScoreImportBatchEntity;
import com.guoyu.platform.competition.entity.CompetitionSimulationScoreImportRowEntity;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionScoreAsyncSchemaContractTest {
  private static final String MIGRATION =
      "db/migration/V20260914_010__competition_score_async_pipeline.sql";

  @Test
  void newAsyncEntitiesHaveCompleteBaseColumnsAndFieldMappings() throws Exception {
    String ddl = migration();
    for (Class<? extends BaseEntity> type : List.of(
        CompetitionFormalScoringTaskEntity.class,
        CompetitionSimulationScoreImportBatchEntity.class,
        CompetitionSimulationScoreImportRowEntity.class)) {
      assertThat(type.getSuperclass()).isEqualTo(BaseEntity.class);
      String table = type.getAnnotation(TableName.class).value();
      String create = createTable(ddl, table);
      assertThat(create).contains("id BIGINT NOT NULL PRIMARY KEY", "tenant_id BIGINT NOT NULL",
          "created_by BIGINT NULL", "updated_by BIGINT NULL", "created_at DATETIME NOT NULL",
          "updated_at DATETIME NOT NULL", "deleted TINYINT NOT NULL DEFAULT 0");
      for (Field field : type.getDeclaredFields()) {
        assertThat(create).as(table + "." + field.getName()).contains(snakeCase(field.getName()));
      }
    }
  }

  @Test
  void migrationDefinesOutboxChannelsLeasesFingerprintsAndIndependentPermissions()
      throws Exception {
    assertThat(migration())
        .contains("event_channel IN ('notice', 'scoring', 'score_pipeline')")
        .contains("uk_comp_formal_scoring_task_revision")
        .contains("idx_comp_formal_scoring_task_recovery")
        .contains("uk_comp_total_score_input")
        .contains("uk_comp_ranking_snapshot_input")
        .contains("uk_comp_ranking_snapshot_published")
        .contains("competition:score:import")
        .contains("competition:score:review")
        .contains("competition:ranking:publish")
        .contains("competition:score-task:retry")
        .doesNotContain("AS new");
  }

  private String migration() throws Exception {
    try (var input = getClass().getClassLoader().getResourceAsStream(MIGRATION)) {
      assertThat(input).as(MIGRATION).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private String createTable(String ddl, String table) {
    int start = ddl.indexOf("CREATE TABLE " + table + " (");
    assertThat(start).as(table).isGreaterThanOrEqualTo(0);
    int end = ddl.indexOf(") ENGINE=InnoDB", start);
    assertThat(end).as(table).isGreaterThan(start);
    return ddl.substring(start, end);
  }

  private String snakeCase(String value) {
    return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
  }
}
