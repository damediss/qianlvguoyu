package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintBindingEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionScoreItemEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CompetitionQuestionImportBatchWriterTest {

  @Test
  void splits1200TheoryRowsIntoSixQuestionAndSixBindingStatements() {
    AtomicInteger questionSql = new AtomicInteger();
    AtomicInteger bindingSql = new AtomicInteger();
    AtomicInteger questionRows = new AtomicInteger();
    AtomicInteger bindingRows = new AtomicInteger();
    CompetitionQuestionImportBatchWriter writer = new CompetitionQuestionImportBatchWriter((sql, args) -> {
      if (sql.startsWith("INSERT INTO competition_question (")) {
        questionSql.incrementAndGet();
        int rows = args.length / CompetitionQuestionImportBatchWriter.QUESTION_COLUMNS;
        questionRows.addAndGet(rows);
        assertThat(rows).isLessThanOrEqualTo(200);
        return rows;
      }
      if (sql.startsWith("INSERT INTO competition_question_blueprint_binding (")) {
        bindingSql.incrementAndGet();
        int rows = args.length / CompetitionQuestionImportBatchWriter.BINDING_COLUMNS;
        bindingRows.addAndGet(rows);
        return rows;
      }
      throw new AssertionError(sql);
    }, 200);

    List<CompetitionQuestionEntity> questions = new ArrayList<>();
    List<CompetitionQuestionBlueprintBindingEntity> bindings = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    for (int i = 1; i <= 1200; i++) {
      CompetitionQuestionEntity question = new CompetitionQuestionEntity();
      question.setId((long) i);
      question.setTenantId(1L);
      question.setEventId(2L);
      question.setEventWorkTypeId(3L);
      question.setEventWorkTypeLevelId(4L);
      question.setContentType("theory");
      question.setQuestionType("single");
      question.setSortOrder(i);
      question.setStem("题干" + i);
      question.setScore(BigDecimal.ONE);
      question.setReferenceAnswer("A");
      question.setOptionsJson("{\"A\":\"甲\",\"B\":\"乙\"}");
      question.setStatus("enabled");
      question.setCreatedAt(now);
      question.setUpdatedAt(now);
      question.setDeleted(0);
      questions.add(question);
      CompetitionQuestionBlueprintBindingEntity binding = new CompetitionQuestionBlueprintBindingEntity();
      binding.setId(10_000L + i);
      binding.setTenantId(1L);
      binding.setQuestionId((long) i);
      binding.setBlueprintId(9L);
      binding.setBlueprintNodeId(8L);
      binding.setMatchMode("full_code");
      binding.setCreatedAt(now);
      binding.setUpdatedAt(now);
      binding.setDeleted(0);
      bindings.add(binding);
    }

    var questionFlush = writer.insertQuestions(questions);
    var bindingFlush = writer.insertBindings(bindings);

    assertThat(questionFlush.batchCount()).isEqualTo(6);
    assertThat(bindingFlush.batchCount()).isEqualTo(6);
    assertThat(questionSql.get() + bindingSql.get()).isEqualTo(12);
    assertThat(questionRows.get()).isEqualTo(1200);
    assertThat(bindingRows.get()).isEqualTo(1200);
  }

  @Test
  void valuesClauseUsesOnePreparedGroupPerRow() {
    assertThat(CompetitionQuestionImportBatchWriter.valuesClause(2, 3)).isEqualTo("(?,?,?),(?,?,?)");
  }

  @Test
  void scoreItemColumnCountMatchesSqlAndArgs() {
    assertThat(CompetitionQuestionImportBatchWriter.SCORE_ITEM_COLUMNS).isEqualTo(20);
    assertThat(CompetitionQuestionImportBatchWriter.QUESTION_COLUMNS).isEqualTo(21);
    assertThat(CompetitionQuestionImportBatchWriter.DETAIL_COLUMNS).isEqualTo(20);
    assertThat(CompetitionQuestionImportBatchWriter.CONTENT_COLUMNS).isEqualTo(10);
    assertThat(CompetitionQuestionImportBatchWriter.BINDING_COLUMNS).isEqualTo(12);

    AtomicInteger scoreItemWidth = new AtomicInteger();
    CompetitionQuestionImportBatchWriter writer = new CompetitionQuestionImportBatchWriter((sql, args) -> {
      scoreItemWidth.set(args.length);
      return 1;
    }, 200);
    var row = new CompetitionPracticalQuestionScoreItemEntity();
    row.setId(1L);
    row.setTenantId(1L);
    row.setQuestionId(2L);
    row.setContentId(3L);
    row.setElementOrder(1);
    row.setItemNo(1);
    row.setItemName("要素");
    row.setElementHtml("<p>要素</p>");
    row.setAllocationEnabled(1);
    row.setCriterion("标准");
    row.setStandardHtml("<p>标准</p>");
    row.setMaxScore(java.math.BigDecimal.TEN);
    row.setStatus("enabled");
    row.setCreatedAt(java.time.LocalDateTime.now());
    row.setUpdatedAt(java.time.LocalDateTime.now());
    row.setDeleted(0);
    writer.insertScoreItems(List.of(row));
    assertThat(scoreItemWidth.get()).isEqualTo(CompetitionQuestionImportBatchWriter.SCORE_ITEM_COLUMNS);
  }
}
