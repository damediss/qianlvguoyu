package com.guoyu.platform.competition.domain;

import com.guoyu.platform.competition.dto.CompetitionPracticalExecutionConfigSaveReq;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperSnapshotEntity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CompetitionPracticalExecutionConfigValidator {
  private static final List<String> START_MODES = List.of("UNIFIED_START", "MANUAL_START");
  private static final List<String> OVERTIME_RULES = List.of("NONE", "STEP_DEDUCTION", "FIXED_DEDUCTION");
  private static final List<String> EARLY_RULES = List.of("NONE", "STEP_BONUS", "FIXED_BONUS");
  private static final List<String> OVERTIME_ACTIONS = List.of("ALLOW_CONTINUE", "AUTO_FINISH");

  public List<String> validate(CompetitionPracticalExecutionConfigSaveReq req,
      CompetitionMakeExamPaperSnapshotEntity scheme, boolean hasPracticalContent) {
    List<String> errors = new ArrayList<>();
    if (req == null) return List.of("实操配置内容不能为空");
    if (!hasPracticalContent) errors.add("当前赛段未启用实操内容，不能配置实操考试");
    if (scheme == null || !"practical".equals(scheme.getContentType())
        || !"published".equals(scheme.getStatus()) || scheme.getQuestionCount() == null
        || scheme.getQuestionCount() <= 0 || scheme.getTotalScore() == null
        || scheme.getTotalScore().signum() <= 0) {
      errors.add("请选择有效且已发布的实操评分方案");
    }
    if (req.durationSeconds() == null || req.durationSeconds() <= 0) {
      errors.add("标准考核时长必须大于0");
    }
    if (!allowed(START_MODES, req.startMode())) errors.add("开始方式无效");
    if (!allowed(OVERTIME_RULES, req.overtimeRuleType())) errors.add("超时扣分规则无效");
    if (!allowed(EARLY_RULES, req.earlyFinishRuleType())) errors.add("提前完成奖励规则无效");
    if (!allowed(OVERTIME_ACTIONS, req.overtimeAction())) errors.add("超时后的处理方式无效");

    if (req.timeScoringEnabled()) {
      if ("STEP_DEDUCTION".equals(req.overtimeRuleType())) {
        positive(req.overtimeStepSeconds(), "超时步长必须大于0", errors);
        nonNegative(req.overtimeScorePerStep(), "每步扣分不能为负数", errors);
        nonNegative(req.overtimeMaxDeduction(), "最大扣分不能为负数", errors);
      } else if ("FIXED_DEDUCTION".equals(req.overtimeRuleType())) {
        nonNegative(req.overtimeFixedDeduction(), "固定扣分不能为负数", errors);
      }
      if ("STEP_BONUS".equals(req.earlyFinishRuleType())) {
        positive(req.earlyFinishStepSeconds(), "提前时间步长必须大于0", errors);
        nonNegative(req.earlyFinishScorePerStep(), "每步加分不能为负数", errors);
        nonNegative(req.earlyFinishMaxBonus(), "最大奖励分不能为负数", errors);
      } else if ("FIXED_BONUS".equals(req.earlyFinishRuleType())) {
        nonNegative(req.earlyFinishFixedBonus(), "固定奖励分不能为负数", errors);
      }
    }

    if ("AUTO_FINISH".equals(req.overtimeAction())) {
      if (req.maxDurationSeconds() == null || req.maxDurationSeconds() <= 0) {
        errors.add("自动结束必须配置最大允许时间");
      } else if (req.durationSeconds() != null && req.maxDurationSeconds() < req.durationSeconds()) {
        errors.add("最大允许时间不能小于标准考核时长");
      }
    }
    if (req.finalScoreUpperLimit() == null || req.finalScoreUpperLimit().signum() <= 0) {
      errors.add("最终成绩上限必须大于0");
    } else if (scheme != null && scheme.getTotalScore() != null
        && req.finalScoreUpperLimit().compareTo(scheme.getTotalScore()) > 0) {
      errors.add("最终成绩上限不能高于评分方案标准总分");
    }
    return List.copyOf(errors);
  }

  private void positive(Long value, String message, List<String> errors) {
    if (value == null || value <= 0) errors.add(message);
  }

  private void nonNegative(BigDecimal value, String message, List<String> errors) {
    if (value == null || value.signum() < 0) errors.add(message);
  }

  private boolean allowed(List<String> values, String value) {
    return value != null && values.contains(value);
  }
}
