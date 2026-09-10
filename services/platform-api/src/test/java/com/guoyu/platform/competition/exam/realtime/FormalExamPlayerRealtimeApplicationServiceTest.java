package com.guoyu.platform.competition.exam.realtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.exam.security.CompetitionExamPrincipal;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamAccessCommandMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamAccessSessionMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class FormalExamPlayerRealtimeApplicationServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-10T01:00:00Z"), ZoneId.of("Asia/Shanghai"));

  private final FormalExamWsTicketService ticketService = Mockito.mock(FormalExamWsTicketService.class);
  private final FormalExamRealtimeProperties properties = new FormalExamRealtimeProperties();
  private final CompetitionFormalExamAccessSessionMapper accessSessions =
      Mockito.mock(CompetitionFormalExamAccessSessionMapper.class);
  private final CompetitionFormalExamAccessCommandMapper commands =
      Mockito.mock(CompetitionFormalExamAccessCommandMapper.class);
  private final FormalExamPlayerRealtimeApplicationService service =
      new FormalExamPlayerRealtimeApplicationService(
          ticketService, properties, accessSessions, commands, CLOCK);

  @AfterEach
  void clearSecurity() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void ackUsesBoundGenerationAndIgnoresMismatch() {
    when(commands.markAckedIfIssued(eq(88L), eq(100L), eq(2), any(LocalDateTime.class))).thenReturn(0);

    service.ack(1L, 100L, 2, "88");

    verify(commands).markAckedIfIssued(eq(88L), eq(100L), eq(2), any(LocalDateTime.class));
  }

  @Test
  void ackFromPlayerUsesPrincipalGenerationFence() {
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        new CompetitionExamPrincipal(77L, 1L, 100L, 4, 9001L, 9101L), "", List.of()));
    when(commands.markAckedIfIssued(eq(88L), eq(100L), eq(4), any(LocalDateTime.class))).thenReturn(1);

    service.ackFromPlayer("88");

    verify(commands).markAckedIfIssued(eq(88L), eq(100L), eq(4), any(LocalDateTime.class));
    verify(commands, never()).markAckedIfIssued(eq(88L), eq(100L), eq(1), any(LocalDateTime.class));
  }
}
