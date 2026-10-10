package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionEventMemberServiceTest {

  private final CompetitionEventMemberMapper memberMapper =
      Mockito.mock(CompetitionEventMemberMapper.class);
  private final CompetitionContestantProfileMapper profileMapper =
      Mockito.mock(CompetitionContestantProfileMapper.class);
  private final CompetitionEventMemberService service =
      new CompetitionEventMemberService(memberMapper, profileMapper);

  @Test
  void reusesExistingContestantMemberInsteadOfRejectingSecondItem() {
    CompetitionEventMemberEntity existing = new CompetitionEventMemberEntity();
    existing.setId(6001L);
    existing.setStatus("active");
    when(memberMapper.selectOne(any())).thenReturn(existing);

    CompetitionEventMemberEntity result = service.createContestantMember(
        1L, 1001L, 8001L, 9L, LocalDateTime.of(2026, 9, 8, 10, 0));

    assertThat(result.getId()).isEqualTo(6001L);
  }
}
