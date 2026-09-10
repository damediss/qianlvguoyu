package com.guoyu.platform.competition.exam.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class FormalExamConsoleWebSocketHandlerTest {

  private final FormalExamConsoleRealtimeApplicationService realtime =
      Mockito.mock(FormalExamConsoleRealtimeApplicationService.class);
  private final FormalExamRealtimeHub hub = Mockito.mock(FormalExamRealtimeHub.class);
  private final FormalExamConsoleWebSocketHandler handler =
      new FormalExamConsoleWebSocketHandler(realtime, hub, new ObjectMapper());

  @Test
  void playerTicketCannotBindConsoleSocket() throws Exception {
    WebSocketSession session = Mockito.mock(WebSocketSession.class);
    when(session.getUri()).thenReturn(URI.create("/ws/competition/formal-exam/console?ticket=player-ticket"));
    when(realtime.bind("player-ticket")).thenReturn(null);

    handler.afterConnectionEstablished(session);

    verify(session).close(any(CloseStatus.class));
    verify(hub, never()).register(session);
  }

  @Test
  void subscribePageStoresCandidateIds() throws Exception {
    WebSocketSession session = Mockito.mock(WebSocketSession.class);
    Map<String, Object> attributes = new HashMap<>();
    when(session.getAttributes()).thenReturn(attributes);

    handler.handleTextMessage(session, new TextMessage(
        "{\"type\":\"console.subscribe-page\",\"candidateExamIds\":[\"9101\",\"9102\"]}"));

    assertThat(attributes.get(FormalExamRealtimeHub.ATTR_PAGE_CANDIDATE_IDS))
        .isEqualTo(List.of(9101L, 9102L));
  }
}
