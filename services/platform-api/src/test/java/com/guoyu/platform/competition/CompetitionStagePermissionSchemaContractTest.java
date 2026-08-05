package com.guoyu.platform.competition;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CompetitionStagePermissionSchemaContractTest {

  @Test
  void migrationContainsAllFrozenStagePermissionsAndAdminGrant() throws IOException {
    String migration;
    try (var input = getClass().getClassLoader().getResourceAsStream(
        "db/migration/V20260802_004__seed_competition_stage_planning_permissions.sql")) {
      assertThat(input).isNotNull();
      migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
    assertThat(migration)
        .contains("competition:stage:view")
        .contains("competition:stage:manage")
        .contains("competition:stage-plan:view")
        .contains("competition:stage-plan:manage")
        .contains("competition:stage-plan:publish")
        .contains("competition:stage-plan:exception")
        .contains("competition:stage-roster:manage")
        .contains("competition:stage-session:manage")
        .contains("competition:practical-rule:manage")
        .contains("competition:judge-group:manage")
        .contains("role_id = 2000");
  }
}
