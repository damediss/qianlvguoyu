package com.guoyu.platform.competition;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CompetitionScoringBridgeSchemaContractTest {

  @Test
  void migrationDefinesStableWorkTypeRuleAndCompleteBaseEntityColumns() throws IOException {
    String migration = migration();

    assertThat(migration)
        .contains("CREATE TABLE competition_event_work_type")
        .contains("UNIQUE KEY uk_competition_event_work_type")
        .contains("tenant_id, event_id, work_type_id, deleted")
        .contains("ADD COLUMN event_work_type_id BIGINT NULL")
        .contains("ADD COLUMN work_type_match_status VARCHAR(32) NOT NULL DEFAULT 'unmatched'")
        .contains("work_type_match_status IN ('matched', 'unmatched', 'conflict')")
        .contains("CREATE TABLE competition_score_rule")
        .contains("theory_weight DECIMAL(5,2)")
        .contains("simulation_practical_weight DECIMAL(5,2)")
        .contains("theory_weight IS NOT NULL")
        .contains("simulation_practical_weight IS NOT NULL")
        .contains("tenant_id BIGINT NOT NULL")
        .contains("created_by BIGINT NULL")
        .contains("updated_by BIGINT NULL")
        .contains("created_at DATETIME NOT NULL")
        .contains("updated_at DATETIME NOT NULL")
        .contains("deleted TINYINT NOT NULL DEFAULT 0");
  }

  @Test
  void permissionsAreRestoredBoundToEventMenuAndGrantedToPlatformAdmin() throws IOException {
    String migration = migration();

    assertThat(migration)
        .contains("'competition:stage:view'")
        .contains("'competition:stage:manage'")
        .contains("'competition:registration:view'")
        .contains("'competition:registration:audit'")
        .contains("'competition:event-work-type:view'")
        .contains("'competition:event-work-type:manage'")
        .contains("'competition:score-rule:view'")
        .contains("'competition:score-rule:manage'")
        .contains("'competition:scoring-readiness:view'")
        .contains("code = 'competition_event'")
        .contains("2000");
  }

  @Test
  void entityGettersMatchNewSnakeCaseColumns() {
    Set<String> eventWorkTypeGetters = getters(CompetitionEventWorkTypeEntity.class);
    assertThat(eventWorkTypeGetters)
        .contains("getId", "getTenantId", "getEventId", "getWorkTypeId", "getDisplayName",
            "getStatus", "getSortOrder", "getDeleted", "getCreatedBy", "getUpdatedBy",
            "getCreatedAt", "getUpdatedAt");

    assertThat(getters(CompetitionContestantProfileEntity.class))
        .contains("getEventWorkTypeId", "getWorkTypeMatchStatus", "getWorkTypeMatchNote");
  }

  @Test
  void scoreRuleEntityMatchesEveryBusinessAndBaseEntityDdlColumn() throws IOException {
    TableName tableName = CompetitionScoreRuleEntity.class.getAnnotation(TableName.class);
    assertThat(tableName).isNotNull();
    assertThat(tableName.value()).isEqualTo("competition_score_rule");
    assertThat(BaseEntity.class.isAssignableFrom(CompetitionScoreRuleEntity.class)).isTrue();

    assertThat(getters(CompetitionScoreRuleEntity.class))
        .contains(
            "getId",
            "getTenantId",
            "getEventId",
            "getStageId",
            "getEventWorkTypeId",
            "getScoringSystem",
            "getTheoryTemplate",
            "getTheoryWeight",
            "getSimulationPracticalWeight",
            "getStatus",
            "getRuleVersion",
            "getPublishedAt",
            "getPublishedBy",
            "getCreatedBy",
            "getUpdatedBy",
            "getCreatedAt",
            "getUpdatedAt",
            "getDeleted");

    assertThat(migration())
        .contains(
            "id BIGINT NOT NULL PRIMARY KEY",
            "tenant_id BIGINT NOT NULL",
            "event_id BIGINT NOT NULL",
            "stage_id BIGINT NOT NULL",
            "event_work_type_id BIGINT NOT NULL",
            "scoring_system VARCHAR(16) NOT NULL",
            "theory_template VARCHAR(32) NULL",
            "theory_weight DECIMAL(5,2) NULL",
            "simulation_practical_weight DECIMAL(5,2) NULL",
            "status VARCHAR(32) NOT NULL",
            "rule_version INT NOT NULL",
            "published_at DATETIME NULL",
            "published_by BIGINT NULL",
            "created_by BIGINT NULL",
            "updated_by BIGINT NULL",
            "created_at DATETIME NOT NULL",
            "updated_at DATETIME NOT NULL",
            "deleted TINYINT NOT NULL DEFAULT 0");
  }

  @Test
  void correctiveMigrationKeepsDatabaseTemplateConstraintAlignedWithDomainEnum()
      throws IOException {
    try (var input = getClass().getClassLoader().getResourceAsStream(
        "db/migration/V20260801_005__allow_custom_competition_theory_template.sql")) {
      assertThat(input).isNotNull();
      String migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(migration)
          .contains("DROP CHECK chk_competition_score_rule_template")
          .contains("'THEORY_90', 'THEORY_120', 'CUSTOM'");
    }
  }

  private String migration() throws IOException {
    try (var input = getClass().getClassLoader().getResourceAsStream(
        "db/migration/V20260727_005__bridge_competition_scoring_readiness.sql")) {
      assertThat(input).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private Set<String> getters(Class<?> type) {
    return Arrays.stream(type.getMethods())
        .map(method -> method.getName())
        .filter(name -> name.startsWith("get"))
        .collect(Collectors.toSet());
  }
}
