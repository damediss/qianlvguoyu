package com.guoyu.platform.competition;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateAnswerEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionFormalExamRuntimeSchemaContractTest {

  @Test
  void runtimeEntitiesAndMigrationContainTheCompleteBaseContract() throws IOException {
    String migration = migration();
    assertEntity(
        migration
            + "\n" + readClasspath("db/migration/V20260908_005__add_formal_exam_scoring.sql")
            + "\n" + readClasspath("db/migration/V20260909_005__formal_exam_deadline_claim_indexes.sql"),
        CompetitionFormalExamSessionEntity.class);
    assertEntity(migration + "\n" + scorePipelineMigration(),
        CompetitionFormalExamPaperSnapshotEntity.class);
    assertEntity(migration, CompetitionFormalExamQuestionSnapshotEntity.class);
    assertEntity(
        migration + "\n" + readClasspath("db/migration/V20260908_003__add_formal_exam_force_submit.sql")
            + "\n" + readClasspath("db/migration/V20260908_004__add_formal_exam_absent_and_finish.sql"),
        CompetitionFormalCandidateExamEntity.class);
  }

  @Test
  void migrationKeepsRuntimeIndependentFromSchedulingAndSeedsPermissions() throws IOException {
    String migration = migration();
    assertThat(migration)
        .contains("occupancy_token BIGINT NOT NULL DEFAULT 0")
        .contains("uk_comp_formal_exam_session_active")
        .contains("waiting_enter")
        .contains("competition:exam-session:view")
        .contains("competition:exam-session:prepare")
        .doesNotContain("schedule_version_id")
        .doesNotContain("session_room_id")
        .doesNotContain("CREATE TABLE competition_formal_candidate_answer");
  }

  @Test
  void startPermissionIsSeededSeparatelyAndDoesNotReuseView() throws IOException {
    String startPermission = readClasspath(
        "db/migration/V20260908_001__add_formal_exam_session_start_permission.sql");
    assertThat(startPermission)
        .contains("competition:exam-session:start")
        .contains("开始正式比赛")
        .doesNotContain("CREATE TABLE")
        .doesNotContain("competition:exam-session:view");
  }

  @Test
  void forceSubmitMigrationAddsTraceFieldsAndDedicatedPermission() throws IOException {
    String migration = readClasspath("db/migration/V20260908_003__add_formal_exam_force_submit.sql");
    assertThat(migration)
        .contains("force_submit_reason")
        .contains("force_submitted_by")
        .contains("competition:exam-session:force-submit")
        .doesNotContain("competition:exam-session:view")
        .doesNotContain("ExamSession")
        .doesNotContain("FINISHED");
    String combined = migration() + "\n" + migration;
    for (String column : List.of("force_submit_reason", "force_submitted_by")) {
      assertThat(combined).contains(column);
    }
  }

  @Test
  void forceOfflinePermissionIsSeededSeparatelyAndDoesNotChangeExamStatus() throws IOException {
    String migration = readClasspath(
        "db/migration/V20260909_004__add_formal_exam_force_offline_permission.sql");
    assertThat(migration)
        .contains("competition:exam-session:force-offline")
        .contains("清退正式比赛选手账号")
        .doesNotContain("competition:exam-session:view")
        .doesNotContain("ALTER TABLE")
        .doesNotContain("CREATE TABLE")
        .doesNotContain("forced_offline");
  }

  @Test
  void absentMigrationAddsStatusAndAbsentTimeWithoutSubmitType() throws IOException {
    String migration = readClasspath("db/migration/V20260908_004__add_formal_exam_absent_and_finish.sql");
    assertThat(migration)
        .contains("absent_time")
        .contains("absent_reason")
        .contains("'absent'")
        .contains("chk_comp_formal_candidate_exam_status")
        .doesNotContain("NO_SHOW")
        .doesNotContain("no_show")
        .doesNotContain("submit_type");
  }

  @Test
  void scoringMigrationAddsIndependentStatusAndResultTables() throws IOException {
    String migration = readClasspath("db/migration/V20260908_005__add_formal_exam_scoring.sql");
    assertThat(migration)
        .contains("scoring_status")
        .contains("not_started")
        .contains("competition_formal_candidate_result")
        .contains("competition_formal_candidate_question_score")
        .contains("uk_comp_formal_candidate_result_exam")
        .contains("uk_comp_formal_candidate_question_score")
        .contains("competition:exam-session:score")
        .doesNotContain("SCORED")
        .doesNotContain("status IN ('preparing'")
        .doesNotContain("CREATE TABLE competition_theory_attempt");
    String combined = migration() + "\n" + migration
        + "\n" + readClasspath("db/migration/V20260909_005__formal_exam_deadline_claim_indexes.sql");
    assertEntity(
        combined,
        com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity.class);
    assertEntity(
        migration + "\n" + scorePipelineMigration(),
        com.guoyu.platform.competition.entity.CompetitionFormalCandidateResultEntity.class);
    assertEntity(
        migration,
        com.guoyu.platform.competition.entity.CompetitionFormalCandidateQuestionScoreEntity.class);
  }

  @Test
  void deadlineClaimMigrationAddsSargableDeadlineAndExpireClaim() throws IOException {
    String migration = readClasspath(
        "db/migration/V20260909_005__formal_exam_deadline_claim_indexes.sql");
    assertThat(migration)
        .contains("deadline_at")
        .contains("expire_claim_until")
        .contains("expire_claim_owner")
        .contains("idx_comp_formal_exam_session_deadline")
        .contains("idx_comp_formal_candidate_exam_session_status")
        .contains("idx_comp_formal_candidate_exam_session_player")
        .contains("idx_comp_formal_candidate_exam_list")
        .doesNotContain("AS new");
  }

  @Test
  void candidateAnswerMigrationMatchesEntityAndUniqueConstraint() throws IOException {
    String migration = readClasspath(
        "db/migration/V20260908_002__create_competition_formal_candidate_answer.sql");
    assertEntity(migration, CompetitionFormalCandidateAnswerEntity.class);
    assertThat(migration)
        .contains("uk_comp_formal_candidate_answer_question")
        .contains("answer_version")
        .contains("is_marked")
        .doesNotContain("correct_answers_json")
        .doesNotContain("fill_answer");
  }

  @Test
  void accessSessionMigrationMatchesEntityAndDoesNotStoreRawToken() throws IOException {
    String migration = readClasspath(
        "db/migration/V20260909_006__formal_exam_access_session.sql");
    assertEntity(migration, com.guoyu.platform.competition.entity.CompetitionFormalExamAccessSessionEntity.class);
    assertEntity(migration, com.guoyu.platform.competition.entity.CompetitionFormalExamAccessCommandEntity.class);
    assertThat(migration)
        .contains("token_fingerprint")
        .contains("generation")
        .contains("force_offline")
        .doesNotContain("access_token")
        .doesNotContain("raw_token")
        .doesNotContain("AS new");
  }

  @Test
  void realtimeOutboxMigrationMatchesEntityAndDoesNotStoreSecrets() throws IOException {
    String migration = readClasspath(
        "db/migration/V20260909_007__formal_exam_realtime_outbox.sql");
    assertEntity(migration, com.guoyu.platform.competition.entity.CompetitionFormalExamOutboxEntity.class);
    assertThat(migration)
        .contains("payload_json")
        .contains("event_id")
        .contains("exam_session_id")
        .doesNotContain("access_token")
        .doesNotContain("raw_token")
        .doesNotContain("AS new");
  }

  private void assertEntity(String migration, Class<?> type) {
    String table = type.getAnnotation(TableName.class).value();
    String ddl = tableDdl(migration, table);
    assertThat(type.getSuperclass()).isEqualTo(BaseEntity.class);
    assertThat(ddl).contains(
        "id BIGINT NOT NULL PRIMARY KEY",
        "tenant_id BIGINT NOT NULL",
        "created_by BIGINT NULL",
        "updated_by BIGINT NULL",
        "created_at DATETIME NOT NULL",
        "updated_at DATETIME NOT NULL",
        "deleted TINYINT NOT NULL DEFAULT 0");
    for (Field field : type.getDeclaredFields()) {
      String column = snakeCase(field.getName());
      assertThat(migration.toLowerCase().contains(column))
          .as(table + "." + field.getName()).isTrue();
    }
  }

  private String migration() throws IOException {
    return readClasspath("db/migration/V20260907_005__create_competition_formal_exam_runtime.sql");
  }

  private String scorePipelineMigration() throws IOException {
    return readClasspath("db/migration/V20260914_010__competition_score_async_pipeline.sql");
  }

  private String readClasspath(String path) throws IOException {
    try (var input = getClass().getClassLoader().getResourceAsStream(path)) {
      assertThat(input).as(path).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private String tableDdl(String migration, String table) {
    String lower = migration.toLowerCase();
    int start = lower.indexOf("create table " + table + " (");
    assertThat(start).as(table).isGreaterThanOrEqualTo(0);
    int end = lower.indexOf(") engine=innodb", start);
    assertThat(end).as(table).isGreaterThan(start);
    return migration.substring(start, end);
  }

  private String snakeCase(String value) {
    return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
  }
}
