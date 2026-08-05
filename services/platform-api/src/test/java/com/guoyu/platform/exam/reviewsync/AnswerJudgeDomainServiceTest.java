package com.guoyu.platform.exam.reviewsync.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** §10.1 判分（与 PHP trainsave 前端判分口径对齐） */
class AnswerJudgeDomainServiceTest {

  private static final List<AnswerJudgeDomainService.AnswerOption> SINGLE_OPTIONS = List.of(
      new AnswerJudgeDomainService.AnswerOption("选项A", false),
      new AnswerJudgeDomainService.AnswerOption("选项B", true),
      new AnswerJudgeDomainService.AnswerOption("选项C", false));

  private static final List<AnswerJudgeDomainService.AnswerOption> MULTI_OPTIONS = List.of(
      new AnswerJudgeDomainService.AnswerOption("甲", true),
      new AnswerJudgeDomainService.AnswerOption("乙", true),
      new AnswerJudgeDomainService.AnswerOption("丙", false),
      new AnswerJudgeDomainService.AnswerOption("丁", false));

  @Test
  void singleCorrectWhenContentMatches() {
    assertThat(AnswerJudgeDomainService.judge("single", SINGLE_OPTIONS, "选项B", null).correct()).isTrue();
    assertThat(AnswerJudgeDomainService.judge("single", SINGLE_OPTIONS, "选项A", null).correct()).isFalse();
  }

  @Test
  void judgeSameAsSingle() {
    assertThat(AnswerJudgeDomainService.judge("judge", List.of(
        new AnswerJudgeDomainService.AnswerOption("正确", true),
        new AnswerJudgeDomainService.AnswerOption("错误", false)), "正确", null).correct()).isTrue();
  }

  @Test
  void multiWrongSelectionMakesItWrong() {
    // 选了错误项丙 → 错（错选不得分）
    assertThat(AnswerJudgeDomainService.judge("multi", MULTI_OPTIONS, null, List.of("甲", "丙")).correct())
        .isFalse();
  }

  @Test
  void multiMissingCorrectStillFullScorePerPhpRule() {
    // 少选（只选甲）→ PHP trainsave 口径仍满分（无部分分，开放问题 O7）
    assertThat(AnswerJudgeDomainService.judge("multi", MULTI_OPTIONS, null, List.of("甲")).correct()).isTrue();
    // 全选对 → 满分
    assertThat(AnswerJudgeDomainService.judge("multi", MULTI_OPTIONS, null, List.of("甲", "乙")).correct())
        .isTrue();
  }

  @Test
  void correctAnswerTextJoinsAllCorrectOptions() {
    assertThat(AnswerJudgeDomainService.judge("multi", MULTI_OPTIONS, null, List.of("甲")).correctAnswerText())
        .isEqualTo("甲、乙");
    assertThat(AnswerJudgeDomainService.judge("single", SINGLE_OPTIONS, "选项A", null).correctAnswerText())
        .isEqualTo("选项B");
  }

  // ---------- O7 严格多选判分（模考开关） ----------

  private static final List<AnswerJudgeDomainService.AnswerOption> MULTI_WITH_SCORE = List.of(
      new AnswerJudgeDomainService.AnswerOption("甲", true, new java.math.BigDecimal("0.2")),
      new AnswerJudgeDomainService.AnswerOption("乙", true, new java.math.BigDecimal("0.2")),
      new AnswerJudgeDomainService.AnswerOption("丙", false, java.math.BigDecimal.ZERO),
      new AnswerJudgeDomainService.AnswerOption("丁", false, java.math.BigDecimal.ZERO));

  @Test
  void strictMultiWrongSelectionScoresZero() {
    var result = AnswerJudgeDomainService.judgeStrictMulti(MULTI_WITH_SCORE, List.of("甲", "丙"),
        new java.math.BigDecimal("1.0"));
    assertThat(result.correct()).isFalse();
    assertThat(result.earnedScore()).isEqualByComparingTo(java.math.BigDecimal.ZERO);
  }

  @Test
  void strictMultiFullSelectionGetsQuestionScore() {
    var result = AnswerJudgeDomainService.judgeStrictMulti(MULTI_WITH_SCORE, List.of("甲", "乙"),
        new java.math.BigDecimal("1.0"));
    assertThat(result.correct()).isTrue();
    assertThat(result.earnedScore()).isEqualByComparingTo(new java.math.BigDecimal("1.0"));
  }

  @Test
  void strictMultiPartialSelectionGetsPartialScore() {
    // 少选 1 个正确项（0.2）→ 部分分 0.2、计错（进错题本）
    var result = AnswerJudgeDomainService.judgeStrictMulti(MULTI_WITH_SCORE, List.of("甲"),
        new java.math.BigDecimal("1.0"));
    assertThat(result.correct()).isFalse();
    assertThat(result.earnedScore()).isEqualByComparingTo(new java.math.BigDecimal("0.2"));
  }

  @Test
  void strictMultiGarbageScoreFallsBackToAllOrNothing() {
    // 选项 score 缺失（脏数据）→ 少选不得分
    var result = AnswerJudgeDomainService.judgeStrictMulti(MULTI_OPTIONS, List.of("甲"),
        new java.math.BigDecimal("1.0"));
    assertThat(result.correct()).isFalse();
    assertThat(result.earnedScore()).isEqualByComparingTo(java.math.BigDecimal.ZERO);
  }
}
