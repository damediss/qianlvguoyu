package com.guoyu.platform.competition.exam.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import com.guoyu.platform.competition.vo.CompetitionFormalExamConsoleRealtimeTicketVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class FormalExamConsoleRealtimeApplicationServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-10T01:00:00Z"), ZoneId.of("Asia/Shanghai"));

  private final FormalExamWsTicketService tickets = Mockito.mock(FormalExamWsTicketService.class);
  private final FormalExamRealtimeProperties properties = new FormalExamRealtimeProperties();
  private final CompetitionFormalExamSessionMapper sessions =
      Mockito.mock(CompetitionFormalExamSessionMapper.class);
  private final FormalExamConsoleRealtimeApplicationService service =
      new FormalExamConsoleRealtimeApplicationService(tickets, properties, sessions, CLOCK);

  @AfterEach
  void clearSecurity() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void issueTicketUsesCurrentUserAndSession() {
    authenticate();
    CompetitionFormalExamSessionEntity session = new CompetitionFormalExamSessionEntity();
    session.setId(9001L);
    session.setTenantId(1L);
    when(sessions.selectById(9001L)).thenReturn(session);
    when(tickets.ttlSeconds()).thenReturn(45);
    when(tickets.issueConsole(any())).thenReturn("console-ticket");

    CompetitionFormalExamConsoleRealtimeTicketVO vo = service.issueTicket(9001L);

    assertThat(vo.ticket()).isEqualTo("console-ticket");
    assertThat(vo.wsPath()).isEqualTo("/api/ws/competition/formal-exam/console");
    assertThat(vo.expiresIn()).isEqualTo(45);
    verify(tickets).issueConsole(new FormalExamWsTicketService.ConsoleTicket(
        1L, 1000L, 9001L, Instant.parse("2026-09-10T01:00:45Z")));
  }

  @Test
  void bindRejectsUnknownSession() {
    FormalExamWsTicketService.ConsoleTicket ticket =
        new FormalExamWsTicketService.ConsoleTicket(1L, 1000L, 9001L, Instant.now().plusSeconds(30));
    when(tickets.consumeConsole("t1")).thenReturn(ticket);
    when(sessions.selectById(9001L)).thenReturn(null);

    assertThat(service.bind("t1")).isNull();
  }

  @Test
  void disabledRealtimeReturnsUnavailable() {
    authenticate();
    properties.setEnabled(false);
    assertThatThrownBy(() -> service.issueTicket(9001L))
        .isInstanceOf(BusinessException.class)
        .hasMessage("实时通道暂时不可用");
  }

  private static void authenticate() {
    var principal = new PlatformUserPrincipal(
        1000L, 1L, "admin", "Admin", "platform", List.of(), List.of(), null);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }
}
