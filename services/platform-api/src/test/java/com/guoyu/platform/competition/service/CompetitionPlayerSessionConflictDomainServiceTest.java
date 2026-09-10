package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.mapper.CompetitionPlayerSessionConflictMapper;
import com.guoyu.platform.competition.mapper.CompetitionPlayerSessionConflictRow;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionPlayerSessionConflictDomainServiceTest {

  private final CompetitionPlayerSessionConflictMapper conflictMapper =
      Mockito.mock(CompetitionPlayerSessionConflictMapper.class);
  private final CompetitionPlayerSessionConflictDomainService service =
      new CompetitionPlayerSessionConflictDomainService(conflictMapper);

  @Test
  void overlappingScheduledSessionIsRejectedWithReadableMessage() {
    LocalDateTime start = LocalDateTime.of(2026, 9, 8, 9, 0);
    LocalDateTime end = LocalDateTime.of(2026, 9, 8, 11, 0);
    CompetitionPlayerSessionConflictRow row = new CompetitionPlayerSessionConflictRow();
    row.setSource("theory");
    row.setEventName("技能大赛");
    row.setWorkTypeName("电工");
    row.setSessionName("A场");
    row.setStartAt(start);
    row.setEndAt(end);
    when(conflictMapper.findFirstOverlap(1L, 8001L, start, end, 5001L, null, null))
        .thenReturn(row);

    assertThatThrownBy(() -> service.assertNoOverlap(1L, 8001L, start, end, 5001L, null, null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("电工")
        .hasMessageContaining("A场");
  }

  @Test
  void incompleteWindowOrEmptyConflictIsIgnored() {
    assertThatCode(() -> service.assertNoOverlap(
        1L, 8001L, LocalDateTime.of(2026, 9, 8, 9, 0), null, null, null, null))
        .doesNotThrowAnyException();
    LocalDateTime start = LocalDateTime.of(2026, 9, 8, 9, 0);
    LocalDateTime end = LocalDateTime.of(2026, 9, 8, 11, 0);
    when(conflictMapper.findFirstOverlap(1L, 8001L, start, end, null, null, null))
        .thenReturn(null);
    assertThatCode(() -> service.assertNoOverlap(1L, 8001L, start, end, null, null, null))
        .doesNotThrowAnyException();
  }
}
