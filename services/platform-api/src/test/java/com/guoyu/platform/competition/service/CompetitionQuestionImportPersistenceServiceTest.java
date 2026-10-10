package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionAssessmentContentMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CompetitionQuestionImportPersistenceServiceTest {

  @Test
  void persists1200BoundTheoryQuestionsInTwelveStatements() {
    AtomicInteger sqlCount = new AtomicInteger();
    CompetitionQuestionImportBatchWriter writer = new CompetitionQuestionImportBatchWriter((sql, args) -> {
      sqlCount.incrementAndGet();
      int columns = sql.contains("competition_question_blueprint_binding")
          ? CompetitionQuestionImportBatchWriter.BINDING_COLUMNS
          : CompetitionQuestionImportBatchWriter.QUESTION_COLUMNS;
      return args.length / columns;
    }, 200);
    CompetitionQuestionImportPersistenceService service = new CompetitionQuestionImportPersistenceService(
        null, null, null, null, null, writer);

    List<CompetitionQuestionWorkbookParser.ParsedQuestion> parsed = new ArrayList<>();
    List<CompetitionQuestionImportPersistenceService.Binding> bindings = new ArrayList<>();
    for (int i = 1; i <= 1200; i++) {
      parsed.add(new CompetitionQuestionWorkbookParser.ParsedQuestion(
          i, "single", "安全操作题干" + i, "A", null,
          "{\"A\":\"甲\",\"B\":\"乙\",\"C\":\"丙\",\"D\":\"丁\"}", 2,
          null, null, null, List.of()));
      bindings.add(new CompetitionQuestionImportPersistenceService.Binding(i - 1, 91L, 92L, "full_code"));
    }

    var stats = service.persist(1L, 7L, 1001L, 3001L, 3101L, "theory", 55L, parsed, bindings);

    assertThat(stats.questionBatchCount()).isEqualTo(6);
    assertThat(stats.bindingBatchCount()).isEqualTo(6);
    assertThat(stats.persistSqlCount()).isEqualTo(12);
    assertThat(sqlCount.get()).isEqualTo(12);
    assertThat(stats.batchWriteMs()).hasSize(12);
  }

  @Test
  void persistsPracticalQuestionWithScoreItemsWithoutRowWidthMismatch() {
    AtomicInteger scoreItemSql = new AtomicInteger();
    CompetitionQuestionImportBatchWriter writer = new CompetitionQuestionImportBatchWriter((sql, args) -> {
      int columns;
      if (sql.contains("competition_question_blueprint_binding")) {
        columns = CompetitionQuestionImportBatchWriter.BINDING_COLUMNS;
      } else if (sql.contains("competition_practical_question_score_item")) {
        columns = CompetitionQuestionImportBatchWriter.SCORE_ITEM_COLUMNS;
        scoreItemSql.incrementAndGet();
      } else if (sql.contains("competition_practical_question_assessment_content")) {
        columns = CompetitionQuestionImportBatchWriter.CONTENT_COLUMNS;
      } else if (sql.contains("competition_practical_question_detail")) {
        columns = CompetitionQuestionImportBatchWriter.DETAIL_COLUMNS;
      } else {
        columns = CompetitionQuestionImportBatchWriter.QUESTION_COLUMNS;
      }
      assertThat(args.length % columns).isZero();
      return args.length / columns;
    }, 200);
    CompetitionQuestionImportPersistenceService service = new CompetitionQuestionImportPersistenceService(
        null, null, mock(CompetitionPracticalQuestionAssessmentContentMapper.class), null, null, writer);

    var scoreItem = new CompetitionQuestionWorkbookParser.ParsedScoreItem(
        1, "评分要素", "评分标准", new BigDecimal("10"), new BigDecimal("10"), BigDecimal.ONE);
    var parsed = List.of(new CompetitionQuestionWorkbookParser.ParsedQuestion(
        1, "practical", "实操题", null, null, null, null,
        "考核要求", "考生准备", "设备准备", List.of(scoreItem),
        60, new BigDecimal("10"), new BigDecimal("100"), "deductive",
        "<p>考核要求</p>", "<p>考场</p>", "<p>考生</p>", null, null,
        List.of(new CompetitionQuestionWorkbookParser.ParsedAssessmentContent(1, "<p>考核内容</p>", List.of(scoreItem))),
        true, false, "A-1-1-1", "一级/二级/考评点", "考评点"));
    var bindings = List.of(new CompetitionQuestionImportPersistenceService.Binding(0, 91L, 92L, "full_code"));

    var stats = service.persist(1L, 7L, 1001L, 3001L, 3101L, "practical", 55L, parsed, bindings);

    assertThat(stats.questionBatchCount()).isEqualTo(1);
    assertThat(stats.bindingBatchCount()).isEqualTo(1);
    assertThat(scoreItemSql.get()).isEqualTo(1);
    assertThat(stats.persistSqlCount()).isEqualTo(5);
  }
}
