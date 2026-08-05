package com.guoyu.platform.competition;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryAnswerEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryAnswerSaveRequestEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryAttemptEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryExamSessionTokenAuditEntity;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionTheoryExamExecutionSchemaContractTest {
  private static final List<Class<? extends BaseEntity>> ENTITIES = List.of(
      CompetitionTheoryAttemptEntity.class,
      CompetitionTheoryAnswerEntity.class,
      CompetitionTheoryAnswerSaveRequestEntity.class,
      CompetitionTheoryExamSessionTokenAuditEntity.class);

  @Test
  void executionTablesMapEveryEntityFieldAndCompleteBaseEntityColumns() throws IOException {
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
            || migration.toLowerCase().contains("add column " + column + " "))
            .as(table + "." + field.getName()).isTrue();
      }
    }
  }

  @Test
  void migrationDefinesIdempotencyStateLocksAndIndependentPermissions() throws IOException {
    assertThat(migration())
        .contains("competition_theory_exam_session_token_audit")
        .contains("UNIQUE KEY uk_comp_theory_attempt_assignment")
        .contains("UNIQUE KEY uk_comp_theory_attempt_submit_request")
        .contains("UNIQUE KEY uk_comp_theory_answer_question")
        .contains("UNIQUE KEY uk_comp_theory_answer_request")
        .contains("status IN ('waiting', 'ready', 'in_progress', 'submitting', 'provisional_absent'")
        .contains("competition:theory-monitor:view")
        .contains("competition:theory-monitor:force-offline")
        .contains("competition:theory-monitor:force-submit")
        .contains("competition:theory-monitor:restore-absence")
        .contains("competition:theory-result:view");
  }

  @Test
  void tokenAuditAndOrdinaryExecutionSchemaDoNotStoreRawTokensOrStandardAnswerCopies()
      throws IOException {
    String migration = migration().toLowerCase();
    assertThat(migration).contains("token_fingerprint").doesNotContain(" token_value ");
    String answerDdl = tableDdl(migration, "competition_theory_answer");
    assertThat(answerDdl).doesNotContain("correct_answers_json", "resolve_content");
  }

  private String migration() throws IOException {
    try (var input = getClass().getClassLoader().getResourceAsStream(
        "db/migration/V20260801_004__create_competition_theory_exam_execution.sql");
         var additiveInput = getClass().getClassLoader().getResourceAsStream(
             "db/migration/V20260802_003__extend_competition_scoring_to_levels.sql")) {
      assertThat(input).isNotNull();
      assertThat(additiveInput).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8)
          + "\n" + new String(additiveInput.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private String tableDdl(String migration, String table) {
    String normalized = migration.toLowerCase();
    int start = normalized.indexOf("create table " + table + " (");
    assertThat(start).as(table + " create statement").isGreaterThanOrEqualTo(0);
    int end = normalized.indexOf(") engine=innodb", start);
    assertThat(end).as(table + " engine clause").isGreaterThan(start);
    return migration.substring(start, end);
  }

  private String snakeCase(String value) {
    return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
  }
}
