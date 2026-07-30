package com.guoyu.platform.competition.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.service.CompetitionScoringReadinessDomainService;
import com.guoyu.platform.competition.vo.CompetitionScoringReadinessVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompetitionScoreRuleReferenceValidatorTest {

  @Mock
  private CompetitionScoringReadinessDomainService readinessDomainService;

  @Test
  void acceptsOnlyUniquePublishedRuleReturnedByExistingReadinessBridge() {
    CompetitionScoringReadinessVO ready = new CompetitionScoringReadinessVO();
    ready.setReady(true);
    ready.setPublishedScoreRuleVersionId(500L);
    when(readinessDomainService.check(1L, 10L, 20L, 30L)).thenReturn(ready);
    CompetitionScoreRuleReferenceValidator validator =
        new CompetitionScoreRuleReferenceValidator(readinessDomainService);

    assertThatCode(() ->
        validator.validatePublishedRule(1L, 10L, 20L, 30L, 500L)).doesNotThrowAnyException();
    assertThatThrownBy(() ->
        validator.validatePublishedRule(1L, 10L, 20L, 30L, 501L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("唯一已发布版本");
  }
}
