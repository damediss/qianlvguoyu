package com.guoyu.platform.competition.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.enums.CompetitionStageStatus;
import org.junit.jupiter.api.Test;

class CompetitionStageLifecycleDomainServiceTest {

  private final CompetitionStageLifecycleDomainService service =
      new CompetitionStageLifecycleDomainService();

  @Test
  void followsTheSevenStateForwardPathAndAllowsClosedReopen() {
    assertThat(service.transition("draft", CompetitionStageStatus.CONFIGURING))
        .isEqualTo(CompetitionStageStatus.CONFIGURING);
    assertThat(service.transition("closed", CompetitionStageStatus.IN_PROGRESS))
        .isEqualTo(CompetitionStageStatus.IN_PROGRESS);
    assertThat(service.transition("closed", CompetitionStageStatus.SETTLED))
        .isEqualTo(CompetitionStageStatus.SETTLED);
  }

  @Test
  void doesNotAllowClientToSkipOrRollbackPublishedResults() {
    assertThatThrownBy(() -> service.transition("draft", CompetitionStageStatus.IN_PROGRESS))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("不允许");
    assertThatThrownBy(() -> service.transition("result_published", CompetitionStageStatus.SETTLED))
        .isInstanceOf(BusinessException.class);
  }
}
