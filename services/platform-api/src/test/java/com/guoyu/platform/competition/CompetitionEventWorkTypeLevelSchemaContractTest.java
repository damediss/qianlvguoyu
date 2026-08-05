package com.guoyu.platform.competition;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CompetitionEventWorkTypeLevelSchemaContractTest {

  @Test
  void levelSnapshotHasBaseFieldsScopeAndImmutableSourceColumns() throws IOException {
    String migration = migration();
    String table = CompetitionEventWorkTypeLevelEntity.class.getAnnotation(TableName.class).value();
    int start = migration.indexOf("CREATE TABLE " + table + " (");
    int end = migration.indexOf(") ENGINE=InnoDB", start);
    assertThat(start).isGreaterThanOrEqualTo(0);
    String ddl = migration.substring(start, end);
    assertThat(ddl).contains(
        "id BIGINT NOT NULL PRIMARY KEY",
        "tenant_id BIGINT NOT NULL",
        "event_work_type_id BIGINT NOT NULL",
        "level_code VARCHAR(16) NOT NULL",
        "source_level_text VARCHAR(255) NULL",
        "created_by BIGINT NULL",
        "updated_by BIGINT NULL",
        "created_at DATETIME NOT NULL",
        "updated_at DATETIME NOT NULL",
        "deleted TINYINT NOT NULL DEFAULT 0",
        "UNIQUE KEY uk_competition_event_work_type_level");
    for (Field field : CompetitionEventWorkTypeLevelEntity.class.getDeclaredFields()) {
      String column = field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
      assertThat(ddl).as(field.getName()).contains("\n  " + column + " ");
    }
  }

  @Test
  void profileBridgeIsNullableForLegacyRows() throws IOException {
    assertThat(migration()).contains("event_work_type_level_id BIGINT NULL");
  }

  private String migration() throws IOException {
    try (var input = getClass().getClassLoader().getResourceAsStream(
        "db/migration/V20260802_002__create_competition_event_work_type_levels.sql")) {
      assertThat(input).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
