package com.guoyu.platform.competition.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
    if (tenantId == null) return;
    final String body;
    try {
      body = objectMapper.writeValueAsString(Map.of(
          "type", type,
          "assignmentId", assignmentId == null ? "" : String.valueOf(assignmentId)));
    } catch (IOException ignored) {
      return;
    }
    TextMessage message = new TextMessage(body);
    for (WebSocketSession session : List.copyOf(sessions)) {
      if (!session.isOpen() || !tenantId.equals(session.getAttributes().get("tenantId"))) continue;
      try {
        synchronized (session) {
          if (session.isOpen()) session.sendMessage(message);
        }
      } catch (Exception ignored) {
        unregister(session);
      }
    }
  }

  public void publishAfterCommit(Long tenantId, String type, Long assignmentId) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      publish(tenantId, type, assignmentId);
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        publish(tenantId, type, assignmentId);
      }
    });
  }
}
