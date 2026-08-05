package com.guoyu.platform.competition.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.competition.dto.CompetitionPracticalRuleItemReq;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionPracticalRuleDomainServiceTest {
  private final CompetitionPracticalRuleDomainService service = new CompetitionPracticalRuleDomainService();

  @Test
  void requiresHundredPointRuleAndValidatesDeduction() {
    assertThat(service.validateAndTotal(List.of(
        new CompetitionPracticalRuleItemReq("a", "A", new BigDecimal("60"), true, null),
        new CompetitionPracticalRuleItemReq("b", "B", new BigDecimal("40"), true, null))))
        .isEqualByComparingTo("100.00");
    assertThat(service.validateEntry(new BigDecimal("80"), new BigDecimal("2"), new BigDecimal("80"), true))
        .isEqualByComparingTo("78.00");
  }

  @Test
  void rejectsDuplicateItemsAndScoresOverItemLimit() {
    assertThatThrownBy(() -> service.validateAndTotal(List.of(
        new CompetitionPracticalRuleItemReq("a", "A", new BigDecimal("50"), true, null),
        new CompetitionPracticalRuleItemReq("a", "A2", new BigDecimal("50"), true, null))))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.validateEntry(new BigDecimal("81"), BigDecimal.ZERO, new BigDecimal("80"), true))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
