package com.guoyu.platform.competition;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CompetitionMultiItemParticipationSchemaContractTest {

  @Test
  void migrationRelaxesEventLevelUniquenessToItemLevel() throws Exception {
    Path modulePath = Path.of(
        "src/main/java/db/migration/V20260908_006__allow_multi_event_work_type_participation.java");
    Path repoPath = Path.of(
        "services/platform-api/src/main/java/db/migration/V20260908_006__allow_multi_event_work_type_participation.java");
    Path path = Files.exists(modulePath) ? modulePath : repoPath;
    String source = Files.readString(path, StandardCharsets.UTF_8);

    assertThat(source)
        .contains("uk_competition_registration_event_member")
        .contains("uk_competition_registration_event_item")
        .contains("uk_competition_contestant_profile_member")
        .contains("uk_competition_contestant_profile_member_item")
        .contains("item_work_type_key")
        .contains("event_work_type_id");
  }
}
