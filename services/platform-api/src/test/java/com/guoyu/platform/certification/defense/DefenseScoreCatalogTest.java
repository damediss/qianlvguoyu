package com.guoyu.platform.certification.defense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.certification.defense.DefenseScoringModels.PerformanceItemScore;
import com.guoyu.platform.certification.defense.DefenseScoringModels.PerformancePayload;
import com.guoyu.platform.certification.defense.DefenseScoringModels.ScoreComputation;
import com.guoyu.platform.certification.defense.DefenseScoringModels.ThesisPayload;
import com.guoyu.platform.common.exception.BusinessException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefenseScoreCatalogTest {

  @Test
  void thesisFinalScoreUsesFortySixtyWeight() {
    ThesisPayload payload = new ThesisPayload(
        "铝电解槽操作实践",
        List.of(false, false, false, false),
        "",
        List.of(15, 15, 15, 15, 15, 15, 10),
        List.of("问题一", "问题二", "问题三"),
        List.of(25, 25, 25, 25)
    );

    ScoreComputation result = DefenseScoreCatalog.computeThesis(payload, true);

    assertThat(result.contentSubtotal()).isEqualByComparingTo("100.00");
    assertThat(result.defenseSubtotal()).isEqualByComparingTo("100.00");
    assertThat(result.finalScore()).isEqualByComparingTo("100.00");
    assertThat(result.rejected()).isFalse();
  }

  @Test
  void thesisRejectRequiresNoteOnSubmit() {
    ThesisPayload payload = new ThesisPayload(
        "题目",
        List.of(true, false, false, false),
        "",
        List.of(10, 10, 10, 10, 10, 10, 8),
        List.of("", "", ""),
        List.of(20, 20, 20, 20)
    );

    assertThatThrownBy(() -> DefenseScoreCatalog.computeThesis(payload, true))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("否定说明");
  }

  @Test
  void thesisScoreCannotExceedMax() {
    ThesisPayload payload = new ThesisPayload(
        "题目",
        List.of(false, false, false, false),
        "",
        List.of(16, 15, 15, 15, 15, 15, 10),
        List.of("", "", ""),
        List.of(25, 25, 25, 25)
    );

    assertThatThrownBy(() -> DefenseScoreCatalog.computeThesis(payload, false))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("满分");
  }

  @Test
  void performanceTotalCapsAtItemMaxAndMarksDisqualify() {
    List<PerformanceItemScore> items = new ArrayList<>();
    items.add(new PerformanceItemScore("duty_ability", 10));
    items.add(new PerformanceItemScore("work_performance", 20));
    items.add(new PerformanceItemScore("papers_textbooks", 20));
    items.add(new PerformanceItemScore("learning", 5));
    items.add(new PerformanceItemScore("training_guidance", 5));
    items.add(new PerformanceItemScore("awards", 10));
    items.add(new PerformanceItemScore("annual_review", 10));
    items.add(new PerformanceItemScore("education", 5));
    items.add(new PerformanceItemScore("tenure", 15));

    ScoreComputation result = DefenseScoreCatalog.computePerformance(new PerformancePayload(items, true, ""), true);

    assertThat(result.rawTotal()).isEqualByComparingTo(BigDecimal.valueOf(100).setScale(2));
    assertThat(result.finalScore()).isEqualByComparingTo("100.00");
    assertThat(result.rejected()).isTrue();
  }

  @Test
  void comprehensiveUsesSixtyFortyWeight() {
    assertThat(DefenseScoreCatalog.comprehensive(new BigDecimal("90.00"), new BigDecimal("80.00")))
        .isEqualByComparingTo("86.00");
  }

  @Test
  void averageIgnoresNullScores() {
    List<BigDecimal> scores = new ArrayList<>();
    scores.add(new BigDecimal("80"));
    scores.add(null);
    scores.add(new BigDecimal("100"));
    assertThat(DefenseScoreCatalog.average(scores)).isEqualByComparingTo("90.00");
  }
}
