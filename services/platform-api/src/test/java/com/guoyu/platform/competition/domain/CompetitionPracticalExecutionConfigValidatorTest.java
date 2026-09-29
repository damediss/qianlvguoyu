package com.guoyu.platform.competition.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.competition.dto.CompetitionPracticalExecutionConfigSaveReq;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperSnapshotEntity;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionPracticalExecutionConfigValidatorTest {
  private final CompetitionPracticalExecutionConfigValidator validator =
      new CompetitionPracticalExecutionConfigValidator();

  @Test
  void rejectsMaximumDurationBelowStandardDuration() {
    List<String> errors = validator.validate(request(5_400L, false, 4_800L), scheme(), true);
    assertTrue(errors.contains("最大允许时间不能小于标准考核时长"));
  }

  @Test
  void allowsAutoFinishWhileTimeScoringIsDisabled() {
    List<String> errors = validator.validate(request(5_400L, false, 6_000L), scheme(), true);
    assertTrue(errors.isEmpty(), () -> String.join("；", errors));
  }

  @Test
  void ignoresIncompleteScoreParametersWhenTimeScoringIsDisabled() {
    CompetitionPracticalExecutionConfigSaveReq req = new CompetitionPracticalExecutionConfigSaveReq(
        10L, 20L, 30L, "实操考试", 5_400L, "UNIFIED_START", true, false,
        "STEP_DEDUCTION", null, null, null, null,
        "STEP_BONUS", null, null, null, null,
        "AUTO_FINISH", 6_000L, new BigDecimal("100"));
    List<String> errors = validator.validate(req, scheme(), true);
    assertTrue(errors.isEmpty(), () -> String.join("；", errors));
  }

  @Test
  void rejectsMissingOrUnpublishedScoringScheme() {
    List<String> errors = validator.validate(request(5_400L, true, null), null, true);
    assertTrue(errors.contains("请选择有效且已发布的实操评分方案"));
  }

  @Test
  void rejectsConfigurationWhenStageDoesNotHavePracticalContent() {
    List<String> errors = validator.validate(request(5_400L, true, null), scheme(), false);
    assertTrue(errors.contains("当前赛段未启用实操内容，不能配置实操考试"));
  }

  @Test
  void rejectsNonStepParametersWhenSelectedStepRuleIsIncomplete() {
    CompetitionPracticalExecutionConfigSaveReq req = new CompetitionPracticalExecutionConfigSaveReq(
        10L, 20L, 30L, "实操考试", 5_400L, "UNIFIED_START", true, true,
        "STEP_DEDUCTION", null, null, null, null, "NONE", null, null, null, null,
        "ALLOW_CONTINUE", null, new BigDecimal("100"));
    List<String> errors = validator.validate(req, scheme(), true);
    assertFalse(errors.isEmpty());
    assertTrue(errors.contains("超时步长必须大于0"));
    assertTrue(errors.contains("每步扣分不能为负数"));
    assertTrue(errors.contains("最大扣分不能为负数"));
  }

  private CompetitionPracticalExecutionConfigSaveReq request(
      Long duration, boolean timeScoringEnabled, Long maxDuration) {
    return new CompetitionPracticalExecutionConfigSaveReq(
        10L, 20L, 30L, "实操考试", duration, "UNIFIED_START", true,
        timeScoringEnabled, "NONE", null, null, null, null,
        "NONE", null, null, null, null, maxDuration == null ? "ALLOW_CONTINUE" : "AUTO_FINISH",
        maxDuration, new BigDecimal("100"));
  }

  private CompetitionMakeExamPaperSnapshotEntity scheme() {
    CompetitionMakeExamPaperSnapshotEntity scheme = new CompetitionMakeExamPaperSnapshotEntity();
    scheme.setContentType("practical");
    scheme.setStatus("published");
    scheme.setQuestionCount(1);
    scheme.setTotalScore(new BigDecimal("100"));
    return scheme;
  }
}
