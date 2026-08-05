package com.guoyu.platform.competition;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import com.guoyu.platform.competition.entity.CompetitionStageContentEntity;
import com.guoyu.platform.competition.entity.CompetitionStageExtensionEntity;
import com.guoyu.platform.competition.entity.CompetitionStageLifecycleHistoryEntity;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionStageLifecycleSchemaContractTest {

  private static final List<Class<? extends BaseEntity>> ENTITIES = List.of(
      CompetitionStageContentEntity.class,
      CompetitionStageExtensionEntity.class,
      CompetitionStageLifecycleHistoryEntity.class);

  @Test
  void lifecycleTablesHaveCompleteBaseEntityColumnsAndMappedFields() throws IOException {
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
        boolean declaredInCreate = ddl.contains("\n  " + column + " ");
        boolean declaredInAlter = migration.toLowerCase().contains("add column " + column + " ");
        assertThat(declaredInCreate || declaredInAlter).as(table + "." + field.getName()).isTrue();
      }
    }
  }

  @Test
  void migrationFreezesFiveStageTypesAndKeepsLifecycleAuditIndexes() throws IOException {
    String migration = migration();
    assertThat(migration)
        .contains("UNIQUE KEY uk_competition_stage_type")
        .contains("idx_competition_stage_lifecycle")
        .contains("idx_competition_stage_history_scope")
        .contains("idx_competition_stage_extension_scope");
  }

  private String migration() throws IOException {
    StringBuilder value = new StringBuilder();
    for (String name : List.of(
        "V20260802_001__create_competition_stage_lifecycle_contract.sql",
        "V20260804_006__add_competition_stage_content_weight.sql",
        "V20260805_003__repair_competition_stage_content_weight.sql")) {
      try (var input = getClass().getClassLoader().getResourceAsStream("db/migration/" + name)) {
        assertThat(input).as(name).isNotNull();
        value.append(new String(input.readAllBytes(), StandardCharsets.UTF_8)).append('\n');
      }
    }
    return value.toString();
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
