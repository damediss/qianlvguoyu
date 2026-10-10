package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionContestantParticipationDomainServiceTest {

  private final CompetitionRegistrationMapper registrationMapper =
      Mockito.mock(CompetitionRegistrationMapper.class);
  private final CompetitionContestantParticipationDomainService service =
      new CompetitionContestantParticipationDomainService(registrationMapper);

  @Test
  void sameEventDifferentWorkTypeIsAllowedWhileSameItemIsRejected() {
    CompetitionEventEntity event = event(1001L, "技能大赛",
        LocalDateTime.of(2026, 9, 1, 8, 0), LocalDateTime.of(2026, 9, 3, 18, 0));
    when(registrationMapper.existsEffectiveByPlayerAndWorkType(1L, 1001L, 8001L, 21L))
        .thenReturn(false);
    when(registrationMapper.selectOtherEffectiveEventsByPlayer(1L, 8001L, 1001L))
        .thenReturn(List.of());

    assertThatCode(() -> service.assertCanEnroll(1L, event, 8001L, 21L))
        .doesNotThrowAnyException();

    when(registrationMapper.existsEffectiveByPlayerAndWorkType(1L, 1001L, 8001L, 20L))
        .thenReturn(true);
    assertThatThrownBy(() -> service.assertCanEnroll(1L, event, 8001L, 20L))
        .isInstanceOf(BusinessException.class)
        .hasMessage("该选手已报名当前赛项");
  }

  @Test
  void overlappingOtherEventIsRejectedWhileAdjacentEventIsAllowed() {
    CompetitionEventEntity current = event(1001L, "秋季赛",
        LocalDateTime.of(2026, 9, 10, 8, 0), LocalDateTime.of(2026, 9, 12, 18, 0));
    when(registrationMapper.existsEffectiveByPlayerAndWorkType(1L, 1001L, 8001L, 21L))
        .thenReturn(false);

    CompetitionEventEntity overlapping = event(2002L, "夏季赛",
        LocalDateTime.of(2026, 9, 11, 8, 0), LocalDateTime.of(2026, 9, 13, 18, 0));
    when(registrationMapper.selectOtherEffectiveEventsByPlayer(1L, 8001L, 1001L))
        .thenReturn(List.of(overlapping));
    assertThatThrownBy(() -> service.assertCanEnroll(1L, current, 8001L, 21L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("夏季赛")
        .hasMessageContaining("时间重叠");

    CompetitionEventEntity adjacent = event(2003L, "春季赛",
        LocalDateTime.of(2026, 9, 1, 8, 0), LocalDateTime.of(2026, 9, 10, 8, 0));
    when(registrationMapper.selectOtherEffectiveEventsByPlayer(1L, 8001L, 1001L))
        .thenReturn(List.of(adjacent));
    assertThatCode(() -> service.assertCanEnroll(1L, current, 8001L, 21L))
        .doesNotThrowAnyException();
  }

  @Test
  void incompleteEventWindowCannotBeCheckedAgainstExistingEnrollment() {
    CompetitionEventEntity current = event(1001L, "未排期赛事", null, null);
    when(registrationMapper.existsEffectiveByPlayerAndWorkType(1L, 1001L, 8001L, 21L))
        .thenReturn(false);
    when(registrationMapper.selectOtherEffectiveEventsByPlayer(1L, 8001L, 1001L))
        .thenReturn(List.of(event(2002L, "已排期赛事",
            LocalDateTime.of(2026, 9, 1, 8, 0), LocalDateTime.of(2026, 9, 3, 18, 0))));

    assertThatThrownBy(() -> service.assertCanEnroll(1L, current, 8001L, 21L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("赛事时间不完整");
  }

  private static CompetitionEventEntity event(
      Long id, String name, LocalDateTime startAt, LocalDateTime endAt) {
    CompetitionEventEntity event = new CompetitionEventEntity();
    event.setId(id);
    event.setName(name);
    event.setStartAt(startAt);
    event.setEndAt(endAt);
    return event;
  }
}
