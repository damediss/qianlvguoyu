package com.guoyu.platform.competition;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CompetitionEventMemberRoleSchemaContractTest {

  @Test
  void definesEventScopedRolesAndProfilesWithCompleteAuditColumns() throws IOException {
    String migration = new String(getClass().getClassLoader().getResourceAsStream(
        "db/migration/V20260727_002__refactor_competition_event_member_roles.sql").readAllBytes(),
        StandardCharsets.UTF_8);

    assertThat(migration).contains("CREATE TABLE competition_event_member")
        .contains("UNIQUE KEY uk_competition_event_member_role")
        .contains("event_id, person_id, role_type, deleted")
        .contains("CREATE TABLE competition_contestant_profile")
        .contains("CREATE TABLE competition_expert_profile")
        .contains("CREATE TABLE competition_referee_profile")
        .contains("CREATE TABLE competition_event_role_conflict_rule")
        .contains("ADD COLUMN member_id BIGINT NULL")
        .contains("tenant_id BIGINT NOT NULL")
        .contains("created_by BIGINT NULL")
        .contains("updated_by BIGINT NULL")
        .contains("created_at DATETIME NOT NULL")
        .contains("updated_at DATETIME NOT NULL")
        .contains("deleted TINYINT NOT NULL DEFAULT 0");
  }
}
