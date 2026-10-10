package com.guoyu.platform.competition.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.common.exception.BusinessException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CompetitionPromotionRuleDomainServiceTest {
  private final CompetitionPromotionRuleDomainService service = new CompetitionPromotionRuleDomainService();

  @Test
  void percentageUsesCeilingAndDoesNotMutateConfiguredRule() {
    var rule = service.resolve("PERCENTAGE", null, 30);

    assertThat(service.calculateTargetCount(7, rule)).isEqualTo(3);
    assertThat(rule.percentage()).isEqualTo(30);
    assertThat(rule.quota()).isNull();
  }

  @Test
  void fixedCountAndLegacyRulesRemainDistinct() {
    var fixed = service.resolve("FIXED_COUNT", 5, null);
    var legacy = service.resolve("LEGACY", null, null);

    assertThat(service.calculateTargetCount(10, fixed)).isEqualTo(5);
    assertThat(legacy.legacy()).isTrue();
    assertThat(service.calculateTargetCount(10, legacy)).isZero();
  }

  @Test
  void finalPodiumRejectsDuplicateMembersAndAllowsShortFieldWithExplanation() {
    assertThatThrownBy(() -> service.validateFinalPodium(Arrays.asList(1L, 1L, 2L), 3, null))
        .isInstanceOf(BusinessException.class);

    service.validateFinalPodium(Arrays.asList(1L, 2L, null), 2, "有效选手不足三人");
  }

  @Test
  void invalidPercentageAndFinalPodiumParametersAreRejected() {
    assertThatThrownBy(() -> service.resolve("PERCENTAGE", null, 0))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> service.resolve("FINAL_PODIUM", 1, null))
        .isInstanceOf(BusinessException.class);
  }
}
