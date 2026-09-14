package com.guoyu.platform.competition.exam.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class FormalExamRealtimeHubTest {

  private final FormalExamRealtimeProperties properties = new FormalExamRealtimeProperties();
  private final FormalExamPresenceStore presenceStore = Mockito.mock(FormalExamPresenceStore.class);
  private final FormalExamRealtimeHub hub = new FormalExamRealtimeHub(properties, presenceStore);

  @Test
  void accessSessionEventsDoNotCrossConnections() throws IOException {
    WebSocketSession own = session("own", 100L, 9001L, 2);
    WebSocketSession other = session("other", 101L, 9001L, 2);
    hub.register(own);
    hub.register(other);

    FormalExamRealtimeEnvelope envelope = new FormalExamRealtimeEnvelope(
        "e1", "c1", FormalExamRealtimeTypes.ACCESS_SESSION_KICKED,
        "100", "9001", "9101", "100", 2, 1L, null, null,
        FormalExamRealtimeTypes.TARGET_ACCESS, Map.of("reason", "kicked"));

    assertThat(hub.deliver(envelope, "{\"type\":\"access-session.kicked\"}")).isEqualTo(1);
    verify(own).sendMessage(any(TextMessage.class));
    verify(own).close(any(CloseStatus.class));
    verify(other, never()).sendMessage(any(TextMessage.class));
    verify(other, never()).close(any(CloseStatus.class));
  }

  @Test
  void sessionEventsReachEveryLocalPadInTheExamSession() throws IOException {
    WebSocketSession first = session("a", 100L, 9001L, 1);
    WebSocketSession second = session("b", 101L, 9001L, 1);
    WebSocketSession otherExam = session("c", 102L, 9002L, 1);
    hub.register(first);
    hub.register(second);
    hub.register(otherExam);

    FormalExamRealtimeEnvelope envelope = new FormalExamRealtimeEnvelope(
        "e2", null, FormalExamRealtimeTypes.SESSION_STATUS_CHANGED,
        "9001", "9001", null, null, null, 2L, null, null,
        FormalExamRealtimeTypes.TARGET_SESSION, Map.of("status", "running"));

    assertThat(hub.deliver(envelope, "{\"type\":\"session.status.changed\"}")).isEqualTo(2);
    verify(first).sendMessage(any(TextMessage.class));
    verify(second).sendMessage(any(TextMessage.class));
    verify(otherExam, never()).sendMessage(any(TextMessage.class));
  }

  @Test
  void staleGenerationIsNotDelivered() throws IOException {
    WebSocketSession stale = session("stale", 100L, 9001L, 1);
    hub.register(stale);
    FormalExamRealtimeEnvelope envelope = new FormalExamRealtimeEnvelope(
        "e3", "c1", FormalExamRealtimeTypes.ACCESS_SESSION_KICKED,
        "100", "9001", "9101", "100", 2, 3L, null, null,
        FormalExamRealtimeTypes.TARGET_ACCESS, Map.of());

    assertThat(hub.deliver(envelope, "{}")).isEqualTo(0);
    verify(stale, never()).sendMessage(any(TextMessage.class));
  }

  @Test
  void accessSessionEventsAlsoReachConsoleOfTheSameExamSession() throws IOException {
    WebSocketSession own = session("own", 100L, 9001L, 2);
    WebSocketSession otherPlayer = session("other", 101L, 9001L, 2);
    WebSocketSession console = consoleSession("console", 9001L, List.of());
    WebSocketSession otherConsole = consoleSession("other-console", 9002L, List.of());
    hub.register(own);
    hub.register(otherPlayer);
    hub.register(console);
    hub.register(otherConsole);

    FormalExamRealtimeEnvelope envelope = new FormalExamRealtimeEnvelope(
        "e4", "c1", FormalExamRealtimeTypes.ACCESS_SESSION_KICKED,
        "100", "9001", "9101", "100", 2, 4L, null, null,
        FormalExamRealtimeTypes.TARGET_ACCESS, Map.of("reason", "kicked"));

    assertThat(hub.deliver(envelope, "{\"type\":\"access-session.kicked\"}")).isEqualTo(2);
    verify(own).sendMessage(any(TextMessage.class));
    verify(own).close(any(CloseStatus.class));
    verify(otherPlayer, never()).sendMessage(any(TextMessage.class));
    verify(console).sendMessage(any(TextMessage.class));
    verify(console, never()).close(any(CloseStatus.class));
    verify(otherConsole, never()).sendMessage(any(TextMessage.class));
  }

  @Test
  void consolePageSubscriptionSkipsPresenceButKeepsStatus() throws IOException {
    WebSocketSession console = consoleSession("console", 9001L, List.of(9101L));
    hub.register(console);

    FormalExamRealtimeEnvelope presence = new FormalExamRealtimeEnvelope(
        "e5", null, FormalExamRealtimeTypes.CANDIDATE_PRESENCE_CHANGED,
        "9102", "9001", "9102", "102", 1, null, null, null,
        FormalExamRealtimeTypes.TARGET_CONSOLE, Map.of("connectionState", "online"));
    FormalExamRealtimeEnvelope status = new FormalExamRealtimeEnvelope(
        "e6", null, FormalExamRealtimeTypes.CANDIDATE_STATUS_CHANGED,
        "9102", "9001", "9102", "102", 1, 5L, null, null,
        FormalExamRealtimeTypes.TARGET_ACCESS, Map.of("status", "submitted", "previousStatus", "running"));

    assertThat(hub.deliver(presence, "{\"type\":\"candidate.presence.changed\"}")).isEqualTo(0);
    verify(console, never()).sendMessage(any(TextMessage.class));
    assertThat(hub.deliver(status, "{\"type\":\"candidate.status.changed\"}")).isEqualTo(1);
    verify(console).sendMessage(any(TextMessage.class));
  }

  @Test
  void openConnectionsCountRegisteredSessions() {
    hub.register(session("a", 100L, 9001L, 1));
    hub.register(session("b", 101L, 9001L, 1));
    assertThat(hub.openConnectionCount()).isEqualTo(2);
    assertThat(hub.droppedSendCount()).isEqualTo(0);
  }

  private static WebSocketSession session(
      String id, Long accessSessionId, Long examSessionId, int generation) {
    WebSocketSession session = Mockito.mock(WebSocketSession.class);
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(FormalExamRealtimeHub.ATTR_ACCESS_SESSION_ID, accessSessionId);
    attributes.put(FormalExamRealtimeHub.ATTR_EXAM_SESSION_ID, examSessionId);
    attributes.put(FormalExamRealtimeHub.ATTR_GENERATION, generation);
    attributes.put(FormalExamRealtimeHub.ATTR_CONNECTION_ID, id);
    when(session.getId()).thenReturn(id);
    when(session.getAttributes()).thenReturn(attributes);
    when(session.isOpen()).thenReturn(true);
    return session;
  }

  private static WebSocketSession consoleSession(String id, Long examSessionId, List<Long> pageIds) {
    WebSocketSession session = Mockito.mock(WebSocketSession.class);
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(FormalExamRealtimeHub.ATTR_EXAM_SESSION_ID, examSessionId);
    attributes.put(FormalExamRealtimeHub.ATTR_ROLE, FormalExamRealtimeHub.ROLE_CONSOLE);
    attributes.put(FormalExamRealtimeHub.ATTR_CONNECTION_ID, id);
    attributes.put(FormalExamRealtimeHub.ATTR_PAGE_CANDIDATE_IDS, pageIds);
    when(session.getId()).thenReturn(id);
    when(session.getAttributes()).thenReturn(attributes);
    when(session.isOpen()).thenReturn(true);
    return session;
  }
}
