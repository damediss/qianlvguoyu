package com.guoyu.platform.competition;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalJudgeGroupEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalJudgeMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalRuleEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalRuleItemEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreEntryEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreSheetEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionStageExecutionPlanEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeStageEntity;
import com.guoyu.platform.competition.entity.CompetitionStageFinalPodiumEntity;
import com.guoyu.platform.competition.entity.CompetitionStageReferenceSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionStageReferenceSnapshotItemEntity;
import com.guoyu.platform.competition.entity.CompetitionStageRosterItemEntity;
import com.guoyu.platform.competition.entity.CompetitionStageRosterSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionStagePromotionItemEntity;
import com.guoyu.platform.competition.entity.CompetitionStagePromotionSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionStageSettlementEntity;
import com.guoyu.platform.competition.entity.CompetitionStageSettlementItemEntity;
import com.guoyu.platform.competition.entity.CompetitionStageUnlockOutboxEntity;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionStageExecutionContractSchemaTest {
  private static final List<Class<? extends BaseEntity>> ENTITIES = List.of(
      CompetitionStageExecutionPlanEntity.class, CompetitionStageRosterSnapshotEntity.class,
      CompetitionStageRosterItemEntity.class, CompetitionPracticalRuleEntity.class,
      CompetitionPracticalRuleItemEntity.class, CompetitionPracticalSessionEntity.class,
      CompetitionPracticalJudgeGroupEntity.class, CompetitionPracticalJudgeMemberEntity.class,
      CompetitionPracticalAssignmentEntity.class, CompetitionPracticalScoreSheetEntity.class,
      CompetitionPracticalScoreEntryEntity.class, CompetitionStageSettlementEntity.class,
      CompetitionStageSettlementItemEntity.class, CompetitionStagePromotionSnapshotEntity.class,
      CompetitionStagePromotionItemEntity.class, CompetitionStageUnlockOutboxEntity.class,
      CompetitionEventWorkTypeStageEntity.class, CompetitionStageReferenceSnapshotEntity.class,
      CompetitionStageReferenceSnapshotItemEntity.class, CompetitionStageFinalPodiumEntity.class);

  @Test
  void executionEntitiesHaveCompleteBaseColumnsAndDdlMappings() throws IOException {
    String ddl = migration();
    for (Class<? extends BaseEntity> entity : ENTITIES) {
      String table = entity.getAnnotation(TableName.class).value();
      String tableDdl = tableDdl(ddl, table);
      assertThat(tableDdl).as(table).contains(
          "id BIGINT NOT NULL PRIMARY KEY", "tenant_id BIGINT NOT NULL",
          "created_by BIGINT NULL", "updated_by BIGINT NULL", "created_at DATETIME NOT NULL",
          "updated_at DATETIME NOT NULL", "deleted TINYINT NOT NULL DEFAULT 0");
      for (Field field : entity.getDeclaredFields()) {
        String column = snakeCase(field.getName());
        boolean declaredInCreate = tableDdl.contains("\n  " + column + " ");
        boolean declaredInAlter = ddl.toLowerCase().contains("add column " + column + " ");
        assertThat(declaredInCreate || declaredInAlter)
            .as(table + "." + field.getName()).isTrue();
      }
    }
  }

  @Test
  void practicalAndSettlementDdlEncodeCriticalInvariants() throws IOException {
    String ddl = migration();
    assertThat(ddl).contains("official_count = 3")
        .contains("status IN ('draft', 'submitted_locked', 'unlocked_for_rescore', 'voided')")
        .contains("UNIQUE KEY uk_comp_practical_score_sheet_revision")
        .contains("contestant_status IN ('completed', 'absent', 'no_result')")
        .contains("competition_stage_unlock_outbox")
        .contains("UNIQUE KEY uk_comp_stage_unlock_outbox_member")
        .contains("ADD COLUMN stage_plan_status VARCHAR(32) NOT NULL DEFAULT 'draft'")
        .contains("ADD COLUMN stage_plan_confirmed_by BIGINT NULL")
        .contains("ADD COLUMN stage_plan_confirmed_at DATETIME NULL");
  }

  private String migration() throws IOException {
    StringBuilder value = new StringBuilder();
    for (String name : List.of(
        "V20260803_001_001__repair_competition_stage_execution_contract.sql",
        "V20260803_002__create_competition_practical_execution_contract.sql",
        "V20260803_004__create_competition_settlement_promotion_contract.sql",
        "V20260803_006__freeze_competition_stage_execution_scope.sql",
        "V20260803_007__add_competition_promotion_source_plan.sql",
        "V20260804_005__add_competition_stage_plan_status.sql",
        "V20260805_005__add_competition_promotion_rules_work_type_stages_and_confirmations.sql",
        "V20260825_002__scope_competition_stage_canvas_by_work_type_level.sql",
        "V20260904_003__create_competition_question_bank_and_make_plan.sql",
        "V20260920_001__competition_practical_assessment_and_runtime.sql",
        "V20260920_102__competition_practical_judge_start_gate.sql",
        "V20260920_104__competition_practical_question_submit_status.sql")) {
      try (var input = getClass().getClassLoader().getResourceAsStream("db/migration/" + name)) {
        assertThat(input).as(name).isNotNull();
        value.append(new String(input.readAllBytes(), StandardCharsets.UTF_8)).append('\n');
      }
    }
    return value.toString();
  }

  private String tableDdl(String migration, String table) {
    String lower = migration.toLowerCase();
    int start = lower.indexOf("create table " + table + " (");
    if (start < 0) {
      start = lower.indexOf("create table if not exists " + table + " (");
    }
    assertThat(start).as(table).isGreaterThanOrEqualTo(0);
    int end = lower.indexOf(") engine=innodb", start);
    assertThat(end).as(table).isGreaterThan(start);
    return migration.substring(start, end);
  }

  private String snakeCase(String value) {
    return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
  }
}
