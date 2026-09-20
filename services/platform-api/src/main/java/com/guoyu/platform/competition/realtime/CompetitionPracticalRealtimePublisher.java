package com.guoyu.platform.competition.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class CompetitionPracticalRealtimePublisher {
  private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
  private final ObjectMapper objectMapper;

  public CompetitionPracticalRealtimePublisher(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public void register(WebSocketSession session) { sessions.add(session); }
  public void unregister(WebSocketSession session) { sessions.remove(session); }

  public void publish(Long tenantId, String type, Long assignmentId) {
    try {
      String body = objectMapper.writeValueAsString(Map.of(
          "type", type,
          "assignmentId", assignmentId == null ? "" : String.valueOf(assignmentId)));
      for (WebSocketSession session : sessions) {
        if (session.isOpen() && tenantId.equals(session.getAttributes().get("tenantId"))) {
          session.sendMessage(new TextMessage(body));
        }
      }
    } catch (IOException ignored) {
      // A later reconnect/refetch restores state; failed sockets are removed on close.
    }
  }
}
