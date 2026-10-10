package com.guoyu.platform.competition.exam.realtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalExamAccessSessionEntity;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamAccessSessionMapper;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

class FormalExamPlayerWebSocketHandlerTest {

  private final FormalExamWsTicketService ticketService = Mockito.mock(FormalExamWsTicketService.class);
  private final FormalExamPresenceStore presenceStore = Mockito.mock(FormalExamPresenceStore.class);
  private final FormalExamRealtimeHub hub = Mockito.mock(FormalExamRealtimeHub.class);
  private final FormalExamPlayerRealtimeApplicationService realtimeService =
      Mockito.mock(FormalExamPlayerRealtimeApplicationService.class);
  private final CompetitionFormalExamAccessSessionMapper accessSessions =
      Mockito.mock(CompetitionFormalExamAccessSessionMapper.class);
  private final FormalExamRealtimeProperties properties = new FormalExamRealtimeProperties();
  private final FormalExamPlayerWebSocketHandler handler = new FormalExamPlayerWebSocketHandler(
      ticketService, presenceStore, hub, realtimeService, accessSessions, properties, new ObjectMapper());

  @Test
  void staleGenerationClosesHandshake() throws Exception {
    WebSocketSession session = Mockito.mock(WebSocketSession.class);
    when(session.getUri()).thenReturn(URI.create("/ws/competition/formal-exam/player?ticket=abc"));
    when(ticketService.consume("abc")).thenReturn(new FormalExamWsTicketService.Ticket(
        1L, 77L, 100L, 2, 9001L, 9101L, null));
    CompetitionFormalExamAccessSessionEntity access = new CompetitionFormalExamAccessSessionEntity();
    access.setId(100L);
    access.setTenantId(1L);
    access.setPlayerId(77L);
    access.setExamSessionId(9001L);
    access.setCandidateExamId(9101L);
    access.setGeneration(3);
    access.setStatus("active");
    when(accessSessions.selectById(100L)).thenReturn(access);

    handler.afterConnectionEstablished(session);

    verify(session).close(any(CloseStatus.class));
    verify(hub, never()).register(session);
    verify(presenceStore, never()).put(any(), anyString(), anyString(), any());
  }

  @Test
  void missingTicketClosesHandshake() throws Exception {
    WebSocketSession session = Mockito.mock(WebSocketSession.class);
    when(session.getUri()).thenReturn(URI.create("/ws/competition/formal-exam/player"));

    handler.afterConnectionEstablished(session);

    verify(session).close(any(CloseStatus.class));
    verify(ticketService, never()).consume(any());
  }
}
